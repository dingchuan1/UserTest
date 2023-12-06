//文件上传
//实现逻辑
//1、判断文件大小，大于50MB就实行分片上传
//2、文件分片、md5计算
//3、发起检查请求，把当前文件的hash发送给服务端，检查是否有相同的hash文件
//4、上传进度计算
//5、上传完成后通知后端合并切片
//后端实现
//1、检查接受到的hash是否有相同的文件，并通知前端当前hash是否有未完成的上传
//2、接受切片
//3、合并所有切片

//通常文件切片是将文件切成1MB大小的分块。但是如果是一个10G以上的文件，将会产生1万个以上的切片，由于js是单线程模型
//如果这个切片计算等是在js主线程执行的话，那时候页面都将会被卡死。这时候可以使用【Web Worker】
//Web Worker 的作用，就是为 JavaScript 创造多线程环境，允许主线程创建 Worker 线程，将一些任务分配给后者运行。在主线程运行的同时，Worker 线程在后台运行，两者互不干扰。

//这里使用百度的WebUploader
//这个必须要在uploader实例化前面
WebUploader.Uploader.register({
    'before-send-file':'beforeSendFile',
    'before-send':'beforeSend'
},{
    beforeSendFile:function(file){
        //Deferred()对象在钩子回掉函数中经常要用到，用来处理需要等待的异步操作。
        var task = WebUploader.Base.Deferred();
        //计算文件MD5
        uploader.md5File(file).progress(percentage => {
            console.log("percentage="+percentage * 100);
            $('#progress_'+ file.id).css('display','block');
            $('#readfiletext_'+ file.id).text('正在读取文件');
            $('#progressBar_text_'+ file.id).text('以读取'+percentage+'%');
            $('#progressBar_'+ file.id).css('width', percentage + '%');
        }).then(function (fileMd5){
            console.log("完成");
            file.md5 = fileMd5;

            $('#progressBar_'+ file.id).css('width', '100%');
            $('#progressBar_text_'+ file.id).text('文件读取完成');
            $('#progressBar_text_'+ file.id).css('color','white');
            $('#readfiletext_'+ file.id).text('文件读取完成');
            var timeoutFuns = {
                fun1 : {funname:'settimeoutNoDisplayDot',value1:'progress_'+ file.id,value2:'delbtn_'+ file.id},
                fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '上传中...'},
                fun3 : {funname : 'settimeoutInlineBlockDisplayDot',value1 : 'cancelbtn_'+ file.id,value2:'stopupbtn_'+ file.id}
            }
            timeout2fun(timeoutFuns,3000);
            //这里通过ajax和后台通信根据md5的信息来判断，可实现断点续传
            $.ajax({
                url: 'http://localhost:8080/GetFileState',
                type: "get",
                data: {'filename':file.name,'filemd5':file.md5,'filepath':"\\test\\",'servicesName':"upLoadFile",'checktype':"file"},
                success: function (data) {
                    if(data == "200"){
                        //分片文件在服务器中不存在，就是正常流程

                    }else if(data == "201"){
                        //分片文件在服务器中已存在，标识上传成功并跳过上传过程
                        uploader.skipFile(file);
                        file.pass = true;
                    }else if(data == "202"){
                        //文件为最新文件，需要确认是否覆盖
                        fileAlert(file.id);
                    }else if(data.startsWith("300_")){
                        //部分以上传，返回值为已经上传的分片数
                        var index = data.indexOf("_");
                        var result = data.slice(index+1);
                        file.saveChunks = result;
                    }
                    task.resolve();
                    console.log(data);
                },
                error: function (data) {
                    //rel.push(data);
                }
            })

        });
        return task;
    },
    beforeSend:function(block){
        console.log("beforeSend");
        var file=block.file;
        var task = WebUploader.Base.Deferred();
        uploader.md5File(file,block.start,block.end).progress(percentage => {
            console.log("percentage="+percentage * 100);
        }).then(function (fileMd5){
            console.log("完成");
            block.md5 = fileMd5;

            //这里通过ajax和后台通信根据md5的信息来判断，可实现断点续传
            $.ajax({
                url: 'http://localhost:8080/GetFileState',
                type: "get",
                data: {'filename':file.name,'filemd5':block.md5,'filepath':"\\test\\",'servicesName':"upLoadFile",'checktype':"chunk"},
                success: function (data) {
                    if(data == "200"){
                        //该分片文件在服务器中不存在，就是正常流程

                    }else if(data == "201"){
                        //分片文件在服务器中已存在，标识上传成功并跳过上传过程
                        block.pass = true;
                    }
                    task.resolve();
                    console.log(data);
                },
                error: function (data) {
                    //rel.push(data);
                }
            })

        });
        return task;
    }
})

// 实例化WebUploader
var uploader = WebUploader.create({
    //swf文件路径
    swf: '/js/webuploader/Uploader.swf',
    // 选完文件后是否自动上传。
    auto: false,
    // 指定上传的服务器端地址。
    server: 'http://localhost:8080/UpLoadFile?servicesName=upLoadFile',

    // 后端用来接收上传文件的参数名称。
    fileVal: 'multipartFile',

    // 请求超时时间。
    timeout: 60000, // 60 seconds.

    // 指定选择文件的按钮容器。
    pick: '#filePicker',

    //formData:{"tokenValue":""},
    // 是否启用截图功能。默认：false。使用截图会降低性能，建议仅在需要时启用。
    paste: false,

    // 是否自动分片上传。默认：false。若服务器端支持分片上传，建议设置为true。
    chunked: true,

    //设置分片的大小这里为3Mb
    chunkSize: 1024 * 1024 * 3,

    // 是否开启分片上传自动重试功能。默认：false。若服务器端支持分片上传，建议设置为true。在某些网络环境下可以有效降低500错误。
    chunkRetry: true,

    //允许同时最大上传进程数
    threads: 20,
    //允许在文件传输时提前把下一个文件准备好
    prepareNextFile:true,
    //在WebUploader配置中记得加上compress：false,resize: false,否则组件在分片时会默认将分片压缩，导致总文件的md5与合并后的md5不一致
    compress:false,//不启用压缩
    resize:false//尺寸不变
});
//fileQueued,当有文件添加进来的时候
//当文件被添加的时候，就会计算md5值，计算过程是异步的，且文件越大计算越久。
//md5FlagMap用于存储文件md5计算完成的标志位；多个文件时，分别设置标志位，key是文件名，value是true或false
var md5FlagMap = new Map();

uploader.on('fileQueued', function(file) {
    md5FlagMap.set(file.name,false);//文件md5值默认没计算完成。
    //var deferrde = WebUploader.Deferred();//Deferred()用于监控异步计算文件md5值这个异步操作的执行状态。
    // 在文件列表中添加文件信息。
    var $list = $('#fileList tbody');
    //var list = document.getElementById("fileList").getElementsByTagName("tbody");
    var fileId = file.id;
    var funfileClick = "fileClick("+ fileId.toString() +")";
    var fileSize = displayFileSize(file.size);
    var lihtml = "<tr id='"+ fileId +"' onclick='"+funfileClick+"' class='file-li'>" +
        "<td data-label='文件名'>" +
            "<div >" +
                "<span style='overflow: hidden;'>"+file.name+"</span>" +
            "</div>" +
        "</td>" +
        "<td data-label='文件信息'>" +
            "<div>" +
                "<span>文件大小：</span>" +
                "<span>"+fileSize+"</span>" +
            "</div>" +
            "<div class='text-muted'>" +
                "<span>文件最后修改日：</span>" +
                "<span>"+file.lastModifiedDate+"</span>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div>" +
                "<span id='readfiletext_"+fileId+"'>等待上传</span>" +
            "</div>" +
            "<div id='progress_"+fileId+"' class='progress progress-li' style='display: none'>" +
                "<div id='progressBar_text_"+fileId+"' class='progress-bar-text-li'>以读取0%</div>" +
                "<div id='progressBar_"+fileId+"' class='progress-bar bg-green progress-bar-li' style='width:0%' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100'>" +
                "</div>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div class='progress progress-li'>" +
                "<div id='progressBar_up_text_"+fileId+"' class='progress-bar-text-li'>等待上传</div>" +
                "<div id='progressBar_up_"+fileId+"' class='progress-bar bg-green progress-bar-li' style='width:0%' role='progressbar' aria-valuenow='0' aria-valuemin='0' aria-valuemax='100'>" +
                "</div>" +
            "</div>" +
        "</td>" +
        "<td>" +
            "<div id='delbtn_"+fileId+"' style='width: 70px;' class='btn btn-purple ' onclick='stopPropag(event); delFileById(this);' >" +
                "撤销" +
            "</div>" +
            "<div id='stopupbtn_"+fileId+"' class='btn btn-purple' typeflag='stop' style='width: 80px;display: none' onclick='stopPropag(event);stopUp(this);'>" +
                "暂停上传" +
            "</div>"+
            "<div id='cancelbtn_"+fileId+"' class='btn btn-purple' typeflag='cancel' style='margin-left:10px;width: 80px;display: none' onclick='stopPropag(event);cancelUp(this);'>" +
                "取消上传" +
            "</div>"+
        "</td>" +
        "</tr>";
    var $li = $(lihtml);
    $list.append($li);
    //list[0].innerHTML=lihtml;
    // let hash = CryptoJS.MD5("tewstdyfhfttfhvyfufyfdryhg");
    // let progress = 0;
    // hash.on('data', (data) => {
    //     // data.loaded 是已经处理的数据长度
    //     // data.total 是总数据长度
    //     progress = Math.round((data.loaded / data.total) * 100);
    //     console.log("progress"+progress);
    // });
    // uploader.md5File(file)
    //     .progress(function(percentage) {
    //         console.log("percentage="+percentage);
    //         $('#progressBar_text_'+ file.id).text('以读取'+percentage+'%');
    //         $('#progressBar_'+ file.id).css('width', percentage + '%');
    //     })
    //     .then(function (fileMd5){
    //         console.log("完成");
    //         file.wholeMd5 = fileMd5;
    //         file_md5 = fileMd5;
    //         //deferrde.resolve(file.name);//文件md5值计算完成后，更新状态为已完成，这时deferred.done()被调用。
    //         md5FlagMap.set(name,true);
    //         $('#progressBar_'+ file.id).css('width', '100%');
    //         $('#progressBar_text_'+ file.id).text('文件读取完成');
    //         $('#progressBar_text_'+ file.id).css('color','white');
    //         $('#readfiletext_'+ file.id).text('文件读取完成');
    //         var timeoutFuns = {
    //             fun1 : {funname:'settimeoutRemoveDot',value1:'progress_'+ file.id},
    //             fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '等待上传'}
    //         }
    //         timeout2fun(timeoutFuns,3000);
    //         $('#'+ file.id +' .btn-loading').removeClass("btn-loading");
    //     })
    //     .catch(function(error) {
    //         console.log("Error occurred:", error);
    //     });
        // deferrde.done(function (name){
        //     md5FlagMap.set(name,true);
        //     $('#progressBar_'+ file.id).css('width', '100%');
        //     $('#progressBar_text_'+ file.id).text('文件读取完成');
        //     $('#progressBar_text_'+ file.id).css('color','white');
        //     $('#readfiletext_'+ file.id).text('文件读取完成');
        //     var timeoutFuns = {
        //         fun1 : {funname:'settimeoutRemoveDot',value1:'progress_'+ file.id},
        //         fun2 : {funname : 'settimeoutChangeText',value1 : 'readfiletext_'+ file.id,value2 : '等待上传'}
        //     }
        //     timeout2fun(timeoutFuns,3000);
        //     $('#'+ file.id +' .btn-loading').removeClass("btn-loading");
        //     var delbtnByfileId = document.getElementById("delbtn_"+file.id);
        //     //$('#delbtn_'+ file.id).on('click',delFileById(file.id+'',delbtnByfileId,true));
        //     delbtnByfileId.addEventListener('click',delFileById(file.id+'',delbtnByfileId,true));
        // });

    //return deferrde.promise();

});
//uploadProgress显示进度条
uploader.on('uploadProgress', function(file, percentage) {
    // 更新进度条。
    $('#uploadProgress').show();
    $('#uploadProgressBar').css('width', percentage + '%');
    $('#uploadProgressBar').find('p').text(percentage+'%');
});


//uploadBeforeSend，在分片模式下，当文件的分块在发送前触发
uploader.on('uploadBeforeSend',function(block,data){
    if(block.pass){
       return;
    }
    var file = block.file;
    //data可以携带参数到后端
    data.name = file.name;//文件名字
    data.filemd5Value = file.md5;//文件整体的md5值
    data.blockmd5Value = block.md5;//文件整体的md5值
    data.start = block.start;//分片数据块在整体文件的开始位置
    data.end = block.end;//分片数据块在整体文件的结束位置
    data.chunk = block.chunk;//分片的索引位置
    data.chunks = block.chunks;//整个文件总共分了多少片
    data.filepath = "\\test\\";
});

//确认是否需要覆盖文件提示框
function fileAlert(fileId){
    uploader.stop(fileId);
    const alertPlaceholder = document.getElementById("progressBar_up_text_"+fileId).parentElement;
    const wrapper = document.createElement('div')
    wrapper.innerHTML = [
        `<div class="alert alert-warning alert-dismissible" role="alert">`,
        `   <div>是否需要覆盖之前的旧文件</div>`,
        `   <button id="trueState_"+fileId type="button" class="btn-purple" data-bs-dismiss="alert" onclick="setuploaderState(true,this);stopPropag(event);" aria-label="Close">是</button>`,
        `   <button id="falseState_"+fileId type="button" class="btn-purple" data-bs-dismiss="alert" onclick="setuploaderState(false,this);stopPropag(event);" aria-label="Close">否</button>`,
        `</div>`
    ].join('');
    alertPlaceholder.style.display = 'none';
    alertPlaceholder.parentElement.append(wrapper);

}

//提示框后续操作
function setuploaderState(flag,obj){
    const index = obj.getId.indexOf("_");
    const fileId = obj.getId.slice(index+1);
    const alertPlaceholder = document.getElementById("progressBar_up_text_"+fileId).parentElement;
    if(flag){
        uploader.upload(fileId);
    }else {
        uploader.skipFile(fileId);
    }
    obj.parentElement.style.display = 'none';
    alertPlaceholder.style.display = '';

}

function fileClick(fileid){
    var delfilelistobj = $("#delfilelist");
    if (delfilelistobj.val().contains(fileid)) {
        if(delfilelistobj.val().contains(","+fileid)){
            delfilelistobj.val(delfilelistobj.val().replace(","+fileid, ""));
        }else{
            delfilelistobj.val(delfilelistobj.val().replace(fileid, ""));
        }
        $("#"+fileid).css('background-color','#86b7fe');
    }else{
        if(delfilelistobj.val() == ""){
            delfilelistobj.val(delfilelistobj.val()+fileid);
        }else {
            delfilelistobj.val(delfilelistobj.val()+","+fileid);
        }
        $("#"+fileid).css("background-color","#ffecb5");
    }

}
/*
    obj:传入this

 */
function delFileById(t){
    var fileid = findFileIdByDom(t);
    var flieTr = document.getElementById(fileid+"");
    flieTr.parentNode.removeChild(flieTr);
    uploader.removeFile(fileId,true);
}
//阻止冒泡
function stopPropag(e){
    e.stopPropagation();
}

function delSelectFile(){
    var delfilelistobj = $("#delfilelist");
    var elements = delfilelistobj.val().split(',');
    $.each(elements, function(index, value) {
        $("#"+value).remove();
        uploader.removeFile(value,true);
    });
}

function uploadFile() {

    //  md5FlagMap里面存储有文件md5计算的状态。
    // 同时上传多个文件时，上传前要判断一下以添加的文件md5计算完成没有。
    //如果有未计算完成的，则继续等待计算结果
    //这个可以优化为，那个文件md5计算完成就上传哪个。
    // var uploadFloag = true;
    // md5FlagMap.forEach(function(value,key){
    //     if(!value){
    //         uploadFloag = false;
    //         alert("文件加载中...");
    //     }
    // });
    // if (uploadFloag){
        uploader.upload();
    // }

}

//文件大小的单位转换
function displayFileSize(fileSize) {
    const units = ['bytes', 'KB', 'MB', 'GB'];
    let i = 0;
    while (fileSize >= 1024 && i < 3) {
        fileSize /= 1024;
        i++;
    }
    return fileSize.toFixed(1) + ' ' + units[i];
}



function cancelUp(t){
    var fileid = findFileIdByDom(t);
    var flag = t.getAttribute('typeflag');
    if(flag == "cancel"){
        uploader.cancelFile(fileid);
        settimeoutInlineBlockDisplayDot("delbtn_"+fileid);
        settimeoutNoDisplayDot("stopupbtn_"+fileid);
        t.setAttribute('typeflag','GoOn');
        $(t).text("重新上传");
    }
    if(flag == "GoOn"){
        uploader.upload(fileid);
        settimeoutInlineBlockDisplayDot("stopupbtn_"+fileid);
        settimeoutNoDisplayDot("delbtn_"+fileid);
        t.setAttribute('typeflag','cancel');
        $(t).text("取消上传");
    }



}

function stopUp(t){
    var fileid = findFileIdByDom(t);
    var flag = t.getAttribute('typeflag');
    if(flag == "stop"){
        uploader.stop(fileid);
        t.setAttribute('typeflag','GoOn');
        $(t).text("继续上传");
    }
    if(flag == "GoOn"){
        uploader.upload(fileid);
        t.setAttribute('typeflag','stop');
        $(t).text("暂停上传");
    }

}

function findFileIdByDom(t){
    // 获取元素的ID
    var id = t.id;
    // 截取第一个"_"以后的字符串
    var index = id.indexOf('_');
    if (index !== -1) {
        var fileId = id.slice(index + 1);
    } else {
        var fileId = "";
        console.log("No underscore found.");
    }
    return fileId;
}

//为元素添加事件
function addEventById(id,type,funcname,arm){
    var eventById = document.getElementById(id+"");
    var fun = window[funcname];
    var keyCount = arm.length;
    if (typeof fun === 'function') {
        switch (keyCount){
            case 0:
                eventById.addEventListener(type+"",fun());
                break;
            case 1:
                eventById.addEventListener(type+"",fun(arm[0]));
                break;
            case 2:
                eventById.addEventListener(type+"",fun(arm[0],arm[1]));
                break;
            case 3:
                eventById.addEventListener(type+"",fun(arm[0],arm[1],arm[2]));
                break;
            case 4:
                eventById.addEventListener(type+"",fun(arm[0],arm[1],arm[2],arm[3]));
                break;
        }

    }

}

function settimeoutChangeText(id,text){
    $('#'+id).text(''+text);
}

//不够解耦暂时不用
function settimeoutChangeBtn(btn1,btn2,btn3){
    $('#'+btn1).css('display','none');
    $('#'+btn2).css('display','block');
    $('#'+btn3).css('display','block');
}

function settimeoutNoDisplayDot(...id){
    for (let i = 0; i < id.length; i++) {
        $('#'+id[i]).css('display','none');
    }
}
function settimeoutDisplayDot(...id){
    for (let i = 0; i < id.length; i++) {
        $('#'+id[i]).css('display','block');
    }
}
function settimeoutInlineBlockDisplayDot(...id){
    for (let i = 0; i < id.length; i++) {
        $('#'+id[i]).css('display','inline-block');
    }
}

//这个timeout函数返回一个Promise，这个Promise将在给定的延迟时间后解析，如果函数执行成功，Promise将解析为成功消息。如果在执行函数时抛出错误，Promise将被拒绝并解析为错误消息。
function timeout2fun(funsobj, delay) {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            try {
                for (var key in funsobj) {
                    if (funsobj.hasOwnProperty(key)) {
                        var funObj = funsobj[key];
                        var keyCount = Object.keys(funObj).length;
                        var fun = window[funObj.funname];
                        if (typeof fun === 'function') {
                            switch (keyCount){
                                case 2:
                                    fun(funObj.value1);
                                    break;
                                case 3:
                                    fun(funObj.value1,funObj.value2);
                                    break;
                                case 4:
                                    fun(funObj.value1,funObj.value2,funObj.value3);
                                    break;
                                case 5:
                                    fun(funObj.value1,funObj.value2,funObj.value3,funObj.value4);
                                    break;
                            }
                        }


                    }
                }

                resolve("The function executed successfully.");
            } catch (error) {
                reject(new Error(error.message));
            }
        }, delay);
    });
}

