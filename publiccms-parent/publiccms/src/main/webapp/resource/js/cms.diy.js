if(window.parent!=window && "string" === typeof templatePath ){
    function checkDiy() {
        var parameters = window.location.search.substring(1).split("&");
        for (var i=parameters.length-1;0<=i;i--) {
            var pair = parameters[i].split("=");
            if("diy" === pair[0] ) {
                return true;
            }
        }
        return false;
    }
    if(checkDiy() ) {
        var links=document.getElementsByTagName("a");
        for (var i=0; i<links.length; i++) {
            if("_blank" === links[i].target){
                links[i].target="";
            }
            if(-1 === links[i].href.indexOf("javascript") && -1 === links[i].href.indexOf("diy") ) {
                links[i].href += (-1 === links[i].href.indexOf("?") )?"?diy":"&diy";
            }
        }
        var forms=document.getElementsByTagName("form");
        for (var i=0; i<forms.length; i++) {
            if("post" == forms[i].method||"POST" == forms[i].method){
                forms[i].method="get";
            }
            var input = document.createElement("input");
            input.type="hidden";
            input.name="diy";
            forms[i].appendChild(input);
        }
        var itemType,itemId;
        if("string" === typeof itemString ) {
            var parameters = itemString.split("&");
            for (var i=0; i < parameters.length; i++) {
                var pair = parameters[i].split("=");
                if("itemType" === pair[0] && pair[1] ) {
                    itemType=pair[1];
                } else if ("itemId" === pair[0] && pair[1] ) {
                    itemId=pair[1];
                }
            }
        }
        var scrollEle;
        function scroll(){
            if(scrollEle ) {
                var offset = scrollEle.getBoundingClientRect();
                window.parent.postMessage({diyevent:'scroll',x:offset.x,y:offset.y,width:offset.width,height:offset.height},"*");
            }
        }
        window.parent.postMessage({url:location.href,diyevent:'load',templatePath:templatePath,itemType:itemType,itemId:itemId},"*");
        var diyElements = document.querySelectorAll('[data-diy]');
        for (var i=0; i < diyElements.length; i++) {
            diyElements[i].onmouseenter = function(){
                scrollEle = this;
                var offset = this.getBoundingClientRect();
                window.parent.postMessage({url:location.href,diyevent:'enter',itemType:this.getAttribute("data-diy"),itemId:this.getAttribute("data-diy-id"),x:offset.x,y:offset.y,width:offset.width,height:offset.height,noborder:this.getAttribute("data-diy-norborder")},"*");
                window.addEventListener("scroll", scroll);
            };
            diyElements[i].onmouseleave = function(){
                scrollEle=null;
                window.parent.postMessage({url:location.href,diyevent:'leave'},"*");
                window.removeEventListener("scroll", scroll);
            };
        }
    }
}