#import "ReactNativeShareExtension.h"
#import "React/RCTRootView.h"
#import <MobileCoreServices/MobileCoreServices.h>

static NSString *const FIELD_URI = @"value"; // uri
static NSString *const FIELD_NAME = @"name";
static NSString *const FIELD_TYPE = @"type";
static NSString *const FIELD_SIZE = @"size";

NSExtensionContext* extensionContext;

@implementation ReactNativeShareExtension {
    NSTimer *autoTimer;
    NSString* type;
    NSString* value;
}

- (UIView*) shareView {
    return nil;
}

RCT_EXPORT_MODULE();

- (void)viewDidLoad {
    [super viewDidLoad];

    //object variable for extension doesn't work for react-native. It must be assign to gloabl
    //variable extensionContext. in this way, both exported method can touch extensionContext
    extensionContext = self.extensionContext;

    UIView *rootView = [self shareView];
    if (rootView.backgroundColor == nil) {
        rootView.backgroundColor = [[UIColor alloc] initWithRed:1 green:1 blue:1 alpha:0.1];
    }

    self.view = rootView;
}


RCT_EXPORT_METHOD(close) {
    [extensionContext completeRequestReturningItems:nil
                                  completionHandler:nil];
}


// openURL is not used
RCT_EXPORT_METHOD(openURL:(NSString *)url) {
  UIApplication *application = [UIApplication sharedApplication];
  NSURL *urlToOpen = [NSURL URLWithString:[url stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
  [application openURL:urlToOpen options:@{} completionHandler: nil];
}



RCT_REMAP_METHOD(data,
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
    [self extractDataFromContext: extensionContext withCallback:^(NSMutableArray* metadataArray, NSException* err) {
        if(err) {
            reject(@"error", err.description, nil);
        } else {
            resolve(metadataArray);
        }
    }];
}

- (void)extractDataFromContext:(NSExtensionContext *)context withCallback:(void(^)(NSMutableArray* metadataArray, NSException *exception))callback {
    @try {
        NSExtensionItem *item = [context.inputItems firstObject];
        NSArray *attachments = item.attachments;

        NSMutableArray *results = [NSMutableArray array];
        NSUInteger totalItems = [attachments count];
        
        [attachments enumerateObjectsUsingBlock:^(NSItemProvider *provider, NSUInteger idx, BOOL *stop) {
            BOOL isLast = idx == totalItems - 1;
            
            // TODO add more file types
            if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeImage]) {
                 [provider loadItemForTypeIdentifier:(NSString *)kUTTypeImage options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                     [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                 }];
            } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeMovie]) {
                [provider loadItemForTypeIdentifier:(NSString *)kUTTypeMovie options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                    [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                }];
            } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeVideo]) {
                [provider loadItemForTypeIdentifier:(NSString *)kUTTypeVideo options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                    [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                }];
            } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeAudio]) {
                [provider loadItemForTypeIdentifier:(NSString *)kUTTypeAudio options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                    [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                }];
            } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypeText]) {
                [provider loadItemForTypeIdentifier:(NSString *)kUTTypeText options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                    [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                }];
            } else if ([provider hasItemConformingToTypeIdentifier:(NSString *)kUTTypePDF]) {
                [provider loadItemForTypeIdentifier:(NSString *)kUTTypePDF options:nil completionHandler:^(id<NSSecureCoding> item, NSError *error) {
                    [self processItem:item error:error results:results shouldInvokeCallback:isLast withCallback:callback];
                }];
            } else {
                callback(nil, [NSException exceptionWithName:@"Error" reason:@"Couldn't find provider" userInfo:nil]);
            }
         }];
        
    }
    @catch (NSException *exception) {
        callback(nil, exception);
    }
}


- (void)processItem:(id<NSSecureCoding>) item
                error:(NSError *)error
                results:(NSMutableArray *)results
                shouldInvokeCallback:(BOOL) shouldInvokeCallback
                withCallback:(void(^)(NSMutableArray* metadataArray, NSException *exception))callback  {
    if (error) {
       callback(nil, [NSException exceptionWithName:@"loadItemForTypeIdentifier Error" reason:error.description userInfo:nil]);
    }
    
    NSURL *url = (NSURL *)item;
    NSMutableDictionary* result = [self getMetadataForUrl:url error:&error];
    if (result) {
        [results addObject:result];
        if (shouldInvokeCallback) {
            callback(results, nil);
        }
    } else {
        callback(nil, [NSException exceptionWithName:@"getMetadata Error" reason:error.description userInfo:nil]);
    }
}

- (NSMutableDictionary *)getMetadataForUrl:(NSURL *)url error:(NSError **)error
{
    __block NSMutableDictionary* result = [NSMutableDictionary dictionary];
    
    [url startAccessingSecurityScopedResource];
    
    NSFileCoordinator *coordinator = [[NSFileCoordinator alloc] init];
    __block NSError *fileError;
    
    [coordinator coordinateReadingItemAtURL:url options:NSFileCoordinatorReadingResolvesSymbolicLink error:&fileError byAccessor:^(NSURL *newURL) {
        
        if (!fileError) {
            [result setValue:newURL.absoluteString forKey:FIELD_URI];
            [result setValue:[newURL lastPathComponent] forKey:FIELD_NAME];
            
            NSError *attributesError = nil;
            NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:newURL.path error:&attributesError];
            if(!attributesError) {
                [result setValue:[fileAttributes objectForKey:NSFileSize] forKey:FIELD_SIZE];
            } else {
                NSLog(@"%@", attributesError);
            }
            
            if ( newURL.pathExtension != nil ) {
                CFStringRef extension = (__bridge CFStringRef)[newURL pathExtension];
                CFStringRef uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, extension, NULL);
                CFStringRef mimeType = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType);
                CFRelease(uti);
                
                NSString *mimeTypeString = (__bridge_transfer NSString *)mimeType;
                [result setValue:mimeTypeString forKey:FIELD_TYPE];
            }
        }
    }];
    
    [url stopAccessingSecurityScopedResource];
    
    if (fileError) {
        *error = fileError;
        return nil;
    } else {
        return result;
    }
}

@end
