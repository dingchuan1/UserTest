
    function dengluonclick(){
        var user = $("#userid").val();
        var password = $("#floatingPassword").val();
        if(user == "" || user==null){
            $("#erouser").css("color","red");
            $("#erouser").text("请输入用户名");
            return
        }
        if(password == "" || password == null){
            $("#eropassword").css("color","red");
            $("#eropassword").text("请输入密码");
            return
        }
        var rel = ["select","insert","update","delete","\'","*","\/\*","\\","union","into","load_file","outfile"];
        for (const relKey in rel) {
            if(user.indexOf(rel[relKey]) != -1){
                $("#erouser").css("color","red");
                $("#erouser").text("用户名含有非法字符：" + rel[relKey]);
                return
            }
        }

        //考虑中：用不用Spring cloud gateway网关配置
        var loginJson = {'User':user,'Password':password};
        Action(loginJson);

    }

    function userchange(type){
        if(type == "user"){
            $("#erouser").removeAttr("style");
            $("#eropassword").removeAttr("style");
            $("#erouser").text("用户名");
            //$("#eropassword").html("密码");
        }
        if(type == "password"){
            $("#eropassword").removeAttr("style");
            $("#eropassword").html("密码");
        }
    }

    function wrongUser(){
        $("#erouser").text("请输入正确的用户名");
    }

    function wrongPassword(){
        $("#erouser").text("请输入正确的密码");
    }
