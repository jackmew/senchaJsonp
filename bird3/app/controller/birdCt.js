
Ext.define('bird.controller.birdCt',{
    extend: 'Ext.app.Controller',
    init: function(){
        console.log('bird controller init');
    },
    config:{
        refs:{
            //select button subimt
            jsonpBtn: 'button[action=callajaxjsonp]',
            jsonLocalBtn: 'button[action=calllocalajaxjson]',
            jsonRemoteBtn: 'button[action=callremoteajaxjson]'
        },
        control: {
            //bind event for botton submit
            jsonpBtn:{
                tap: 'doCallAjaxJsonp'
            },
            jsonLocalBtn:{
                tap: 'doCallLocalAjaxJson'
            },
            jsonRemoteBtn:{
                tap: 'doCallRemoteAjaxJson'
            }

        }     
    },  
    doCallAjaxJsonp: function(){
        console.log('press callAjaxJsonp');
        this.testFn();
        this.loginJsonp();
    },
    doCallLocalAjaxJson: function(){
        console.log('press callAjaxJson');
        this.getLocalDataJson();
    },
    doCallRemoteAjaxJson: function(){
        console.log('press doCallRemoteAjaxJson');
        this.getRemoteDataJson();
    },
    testFn: function(){
        console.log("you call the function :testFn");
    },
    getJsonp:function(){
        console.log("getJsonp start");
        Ext.data.JsonP.request({
                url: 'http://192.168.26.115:8080/wbase-web/rest/uid/jsonp',
                crossDomain: true,
                type: "GET",
                dataType: "jsonp",
                
                callbackKey: 'callback',
                scope: this,
                
                success: function(msg,result,request){
                    console.log("getJsonp success msg :"+msg.Spring);
                    this.getRealDataJsonp();
                    
                },
                failure: function(response, opts) {
                    console.log('getJsonp fail' + response);
                }
        });
        
    },
    loginJsonp: function(){
        console.log("login start");
        Ext.data.JsonP.request({
            url: 'http://192.168.26.115:8080/wbase-web/rest/account/loginJsonp',
            crossDomain: true,
            type: "GET",
            dataType: "jsonp",
            params: {
                username: "administrator",
                password: 1234
            },
            callbackKey: 'callback',
            scope: this,
            
            success: function(msg,result,request){
                console.log("login success msg :"+msg);
                console.log("login success msg status :"+msg.status);
                //好像不能連用Ext.Msg.alert()
                //可是可以連用alert()
                this.getJsonp();

            },
            failure: function(response, opts) {
                console.log('login fail :'+response.status);
            }
        });
    },
    getRealDataJsonp:function(){
        console.log("start getRealDataJsonp");
        Ext.data.JsonP.request({
            url: 'http://192.168.26.115:8080/wbase-web/rest/uid/queryOnePageJsonp',
            crossDomain: true,
            type: "GET",
            dataType: "jsonp",
            params: {
                currBlock: 1,
                condition: 'ZBBBBB'
            },
            callbackKey: 'callback',
            scope: this,
            
            success: function(msg,result,request){
                
                console.log("getRealDataJsonp success msg :"+msg);
                var objToJson = JSON.stringify(msg);
                console.log("getRealDataJsonp success msg :"+objToJson);
                //console.log(msg[0].id);
                console.log("----------------------------------------------");
            },
            failure: function(response, opts) {
                console.log('getRealDataJsonp fail :'+response);
            }
        });
    },
    //not useful , but can remind me
    callback6:function(result){
        console.log("Ext.data.JsonP.callback6 "+result);
        console.log("Ext.data.JsonP.callback6 "+result[0].id);
    },
    getLocalDataJson:function(){
        console.log("start getLocalDataJson");
       
        Ext.Ajax.request({
                url: 'app/data/People.json',
                success: function(response, opts) {
                    var obj = Ext.decode(response.responseText);
                    console.log("getLocalDataJson success :"+obj);
                },
                failure: function(response, opts) {
                    console.log('getLocalDataJson failure :' + response);
                }
            });
    },
    getRemoteDataJson:function(){
        console.log("start getRemoteDataJson");
        var senchaThis = this; //這邊的this是sencha的this
                               //沒有設定的話 call ajax後的this指的是window 
        Ext.Ajax.request({
                url: 'http://192.168.26.115:8080/wbase-web/rest/account/login',
                params: {
                    username: "administrator",
                    password: 1234
                },
                method:"GET",
                useDefaultXhrHeader: false,
                success: function(response, opts) {
                    var obj = Ext.decode(response.responseText);
                    console.log("getRemoteDataJson success :"+obj);
                    console.log("getRemoteDataJson 1End ");
                    senchaThis.getRealDataJson();
                    console.log("getRemoteDataJson 2End ");
                },
                failure: function(response, opts) {
                    console.log('getRemoteDataJson failure :' + response);
                    senchaThis.testFn();
                }
            });
    },
    getRealDataJson:function(){
        console.log("start getRealDataJson");
        Ext.Ajax.request({
            url: 'http://192.168.26.115:8080/wbase-web/rest/uid/queryOnePage',
            type: "GET",
            params: {
                currBlock: 1,
                condition: 'ZBBBB'
            },
            method:"GET",
                success: function(response, opts) {
                    console.log("getRealDataJson success");
                    var obj = Ext.decode(response.responseText);
                    console.log("getRemoteDataJson success :"+obj);
                },
                failure: function(response, opts) {
                    console.log('getRealDataJson failure :' + response);
            }
        });
    }

    

});
