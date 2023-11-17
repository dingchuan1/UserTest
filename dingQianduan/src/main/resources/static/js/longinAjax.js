function Action(json) {
    let rel = new Array();
    $.ajax({
        url: 'http://localhost:8080/Login',
        type: "post",
        data: json,
        success: function (data) {
            rel.push(data);
            let info=JSON.parse(data);
            if(info["code"]=="200"){
                document.cookie = info["data"].tokenName+"="+info["data"].tokenValue;
                let page = checkPerm(info["msg"]);
                console.log(page);
                window.location.href="http://localhost:8080/"+page+".html?servicesName="+page;
            }
            console.log(data);
        },
        error: function (data) {
            rel.push(data);
        }
    })

    return rel;
}

function checkPerm(msg){
    //admin
    if(msg.indexOf("admin") != "-1"){
        return "adminIndex";
    }
    //user
    if(msg.indexOf("user") != "-1"){
        return "userindex";
    }
}