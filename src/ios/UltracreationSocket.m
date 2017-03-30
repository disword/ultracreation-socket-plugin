// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "UltracreationSocket.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <netdb.h>
#include <stdarg.h>
#include <string.h>
#include <fcntl.h>


#pragma mark UltracreationSocket interface

@interface UltracreationSocket () {
    NSMutableArray* _sockets;
}
@end


#pragma mark ChromeSocket implementation

@implementation UltracreationSocket

- (void)pluginInitialize
{
    _sockets = [NSMutableArray arrayWithCapacity:5];
}

- (void)destroyAllSockets
{
    NSLog(@"Destroying all open sockets");
    for (int i = 0; i < _sockets.count; i++)
    {
        NSNumber * socketFd = [_sockets objectAtIndex:i];
        close([socketFd intValue]);
    }
    [_sockets removeAllObjects];
}

- (void)onReset
{
    [self destroyAllSockets];
}

- (void)dispose
{
    [self destroyAllSockets];
    [super dispose];
}



-(int)getMaxFd:(NSArray*)array
{
    int maxFd = 0;
    for(int i = 0; i < [array count]; i++)
    {
        NSNumber* temp = [array objectAtIndex:i];
        
        if(maxFd < [temp intValue])
        {
            maxFd = [temp intValue];
        }
    }
    return maxFd;
}

int set_nonblock(int socket)
{
    int flags = fcntl(socket,F_GETFL,0);
    if(flags < 0){
        NSLog(@"Set server socket nonblock flags failed\n");
        return -1;
    }
    
    if (fcntl(socket, F_SETFL, flags | O_NONBLOCK) < 0)
    {
        NSLog(@"Set server socket nonblock failed\n");
        return -1;
    }
    
    return 1;
}

- (void)socket:(CDVInvokedUrlCommand*)command
{
    NSLog(@"socket");
    
    [self.commandDelegate runInBackground:^{
        NSLog(@"runInBackground");
        NSString* socketMode = [command argumentAtIndex:0];
        
        if(socketMode != nil && ([socketMode isEqualToString:@"tcp"] || [socketMode isEqualToString:@"tcp_server"]
                                 || [socketMode isEqualToString:@"udp"]))
        {
            int socketFd;
            if([socketMode isEqualToString:@"udp"])
            {
                socketFd = socket(AF_INET, SOCK_DGRAM, 0);
            }else{
                socketFd = socket(AF_INET, SOCK_STREAM, 0);
            }
            
            if(socketFd < 0)
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
            else
            {
                int block = set_nonblock(socketFd);
                if(block < 0)
                {
                    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
                }else{
                    [_sockets addObject:[NSNumber numberWithInt:socketFd]];
                    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:socketFd] callbackId:command.callbackId];
                }
            }
        }
        
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
    }];
    
}

- (void)bind:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString* info = [command argumentAtIndex:1];
        NSArray *array = [info componentsSeparatedByString:@":"];
        
        NSNumber* socketId = [command argumentAtIndex:0];
        NSString* address = [array objectAtIndex:0];
        int port = [[array objectAtIndex:1] intValue];
        
        
        struct sockaddr_in sockaddr;
        sockaddr.sin_family = AF_INET;
        if ([address isEqualToString:@"0.0.0.0"])
            sockaddr.sin_addr.s_addr = htonl(INADDR_ANY);
        else{
            int rtn = inet_pton(AF_INET, [address UTF8String], &sockaddr.sin_addr.s_addr);
            if(rtn < 0)
            {
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
                return;
            }
        }
        sockaddr.sin_port = htons(port);
        
        int ret = bind([socketId intValue], (struct sockaddr *)&sockaddr, sizeof(sockaddr));
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)listen:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        
        NSNumber* socketId = [command argumentAtIndex:0];
        NSNumber* backlog = [command argumentAtIndex:1];
        
        int ret = listen([socketId intValue], [backlog intValue]);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}


- (void)connect:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSString* info = [command argumentAtIndex:1];
        NSArray *array = [info componentsSeparatedByString:@":"];
        
        NSNumber* socketId = [command argumentAtIndex:0];
        NSString* address = [array objectAtIndex:0];
        int port = [[array objectAtIndex:1] intValue];
        
        
        struct sockaddr_in sockaddr;
        sockaddr.sin_family = AF_INET;
        if ([address isEqualToString:@"0.0.0.0"])
            sockaddr.sin_addr.s_addr = htonl(INADDR_ANY);
        else{
            int rtn = inet_pton(AF_INET, [address UTF8String], &sockaddr.sin_addr.s_addr);
            if(rtn < 0)
            {
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
                return;
            }
        }
        sockaddr.sin_port = htons(port);
        
        int ret = connect([socketId intValue], (struct sockaddr *)&sockaddr, sizeof(sockaddr));
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)accept:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSNumber* socketId = [command argumentAtIndex:0];
        
        struct sockaddr_in sockaddr;
        int ret = accept([socketId intValue], (struct sockaddr *)&sockaddr, (socklen_t*)&sockaddr);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}



- (void)select:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        NSArray* selectSet = [command argumentAtIndex:0];
        int time = [[command argumentAtIndex:1] intValue];
        
        int maxFd = [self getMaxFd:selectSet];
        
        fd_set readfds;
        FD_ZERO(&readfds); //clear the socket set
        
        for(int i = 0; i < [selectSet count]; i++)
        {
            NSNumber* temp = [selectSet objectAtIndex:i];
            FD_SET([temp intValue],&readfds);
        }
        
        
        int ret;
        struct timeval tv;
        if(time < 0)
        {
            ret = select(maxFd + 1, &readfds , NULL , NULL , NULL);
        }else
        {
            tv.tv_sec = time / 1000;
            tv.tv_usec = (time % 1000) * 1000;
            ret = select(maxFd + 1, &readfds , NULL , NULL , &tv);
        }
        
        
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }
        else
        {
            NSMutableArray* result = [NSMutableArray arrayWithCapacity:[selectSet count]];
            for(int i = 0; i < [selectSet count]; i++)
            {
                NSNumber* temp = [selectSet objectAtIndex:i];
                if (FD_ISSET([temp intValue], &readfds))
                {
                    [result addObject:temp];
                }
            }
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:result] callbackId:command.callbackId];
        }
    }];
}

- (void)send:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        NSData* data = [command argumentAtIndex:1];
        
        const char* buffer = [data bytes];
        int ret = send(socketId, buffer, strlen(buffer), 0);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}



- (void)recv:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        int bufferSize = [[command argumentAtIndex:1] intValue];
        
        char buffer[bufferSize];
        ssize_t ret = recv(socketId , buffer, sizeof(buffer)-1, 0);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }
        else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArrayBuffer:[NSData dataWithBytes:buffer length:strlen(buffer)]] callbackId:command.callbackId];
        }
    }];
}

- (void)close:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        
        int ret = close(socketId);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)shutdown:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        
        int socketId = [[command argumentAtIndex:0] intValue];
        int how = [[command argumentAtIndex:1] intValue];
        int ret = shutdown(socketId, how);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)setreuseraddr:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        int enable = [[command argumentAtIndex:1] intValue];
        NSLog(@"setreuseraddr = %d",enable);
        int ret = setsockopt(socketId, SOL_SOCKET, SO_REUSEADDR, (const char*)&enable, sizeof(enable));
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)setbroadcast:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        int enable = [[command argumentAtIndex:1] intValue];
        NSLog(@"setbroadcast = %d",enable);
        int ret = setsockopt(socketId, SOL_SOCKET, SO_BROADCAST, (const char*)&enable, sizeof(enable));
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:ret] callbackId:command.callbackId];
        }
    }];
}

- (void)getsockname:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        
        struct sockaddr_in sin;
        socklen_t len = sizeof(sin);
        
        int ret = getsockname(socketId, (struct sockaddr *)&sin, &len);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }else
        {
            NSString* address = [NSString stringWithUTF8String: inet_ntoa(sin.sin_addr)];
            NSString* port = [NSString stringWithFormat:@"%d", ntohs(sin.sin_port)];
            NSString* result = [address stringByAppendingFormat:@"%@%@",@":",port];
            NSLog(@"result = %@",result);
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result] callbackId:command.callbackId];
        }
    }];
}

- (void)getpeername:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        int socketId = [[command argumentAtIndex:0] intValue];
        
        struct sockaddr_in peeraddr;
        socklen_t len;
        
        int ret = getpeername(socketId, (struct sockaddr *)&peeraddr, &len);
        if(ret < 0)
        {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsInt:errno] callbackId:command.callbackId];
        }
        else
        {
            NSString* address = [NSString stringWithUTF8String: inet_ntoa(peeraddr.sin_addr)];
            NSString* port = [NSString stringWithFormat:@"%d", ntohs(peeraddr.sin_port)];
            NSString* result = [address stringByAppendingFormat:@"%@%@",@":",port];
            NSLog(@"result = %@",result);
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result] callbackId:command.callbackId];
        }
    }];
}


@end
