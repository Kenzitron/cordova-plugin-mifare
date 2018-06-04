"use strict";

var mifare ={
    readUID: function(onSuccess, onError, args){
        cordova.exec(
            onSuccess,
            onError,
            "MifarePlugin", 
            "readUID", 
            [args]
        );
    },
     closeReadUID: function(onSuccess, onError, args){
         cordova.exec(
             onSuccess,
             onError,
             "MifarePlugin",
             "closeReadUID",
             [args]
         );
     },
}

window.mifare=mifare;