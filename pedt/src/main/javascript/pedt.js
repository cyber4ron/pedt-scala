
var pedt_scala_obj = Java.type("com.wandoujia.n4c.pedt.core.PEDT4JS"); // scala companion obj

var Base64 = {_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/\r\n/g,"\n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}

var pedt_scala = {
    query: function(scope) {
        return pedt_scala_obj.query(scope);
    },

    download_task: function(task_id) {
        var value = JSON.parse(pedt_scala_obj.fetchTask(task_id));
        try {
            return JSON.parse(value)
        } catch(err) {
            return value
        }
    },

    execute_task: function(task, args_json) {
        if(typeof task == 'function') {
            return pedt_scala_obj.run("script:javascript:base64:" + Base64.encode(task.toString()), JSON.stringify(args_json));
        } else if(typeof task == 'string' && task.substr(0, 7) == "script:") { // script
            return pedt_scala_obj.run(task, JSON.stringify(args_json));
        } else if(typeof task == 'string') { // task_id
            return pedt_scala_obj.runTask(task, JSON.stringify(args_json));
        } else {
            return "undefined";
        }
    },

    run: function(task, args_json) {
        return this.execute_task(task, args_json);
    },

    map: function(scope, task_id, args_json) {
        return pedt_scala_obj.map(scope, task_id, JSON.stringify(args_json));
    },

    reduce: function(scope, task_id, args_json, reduce_task) {
        if(typeof reduce_task == 'function') {
            return pedt_scala_obj.reduce(scope, task_id, JSON.stringify(args_json),
                "script:javascript:base64:" + Base64.encode(reduce_task.toString()));
        } else if(typeof reduce_task == 'string') { // script
            return pedt_scala_obj.reduce(scope, task_id, JSON.stringify(args_json), reduce_task);
        } else {
            return "undefined";
        }
    },

    daemon: function(scope, task_id, daemon_task, daemon_task_args_json) {
        if(typeof daemon_task == 'function') {
            return pedt_scala_obj.daemon(scope, task_id, JSON.stringify(daemon_task_args_json),
                "script:javascript:base64:" + Base64.encode(daemon_task.toString()),
                daemon_task_args_json);
        } else if(typeof daemon_task == 'string') { // script
            return pedt_scala_obj.daemon(scope, task_id, JSON.stringify(daemon_task_args_json), daemon_task);
        } else {
            return "undefined";
        }
    },

    subscribe: function(scope) {
        return pedt_scala_obj.subscribe(scope);
    },

    wait_within: function(future, duration_ms) {
        return pedt_scala_obj.waitWithin(future, duration_ms);
    }
};
