var keys;
ecodeSDK.overwritePropsFnQueueMapSet('Table', {
    fn: (newProps) => {
        const { hash } = window.location;
        if (hash.startsWith('#/main/cube/search?customid=173&_key')) {
            newProps.rowSelection = {
                getCheckboxProps: record => ({
                    disabled: record.ztzt == '1',    // 配置无法勾选的列
                }),
                onChange: (selectedRowKeys) => {
                    keys = selectedRowKeys;
                }
            };
            var fun={
                render: (value, row, index) => <div dangerouslySetInnerHTML = {{__html:value}} ></div>
            }
            var obj={ ...newProps.columns[4], ...fun }
            newProps.columns[4]=obj



        }

        return newProps;
    }
});
ecodeSDK.overwriteClassFnQueueMapSet('WeaTop', {
    fn: (Com, newProps) => {
        const { Button, message } = antd;
        const { hash } = window.location;
        if (!hash.startsWith('#/main/cube/search?customid=173&_key')) return;
        submit = () => {

            if (keys.length == 1) {
                ecCom.WeaLoadingGlobal.start() ;
                ModeForm.controlBtnDisabled(true);
                ModeForm.controlBtnDisabled(false);
                WeaTools.callApi('/api/archives/anewPushArchives', 'GET', { ids: keys }).then(resp => {
                    if(resp.code==200){
                        message.success("发送成功", 3);
                    }else{
                        message.error("发送失败", 3);
                    }
                    ModeForm.controlBtnDisabled(true)
                    ecCom.WeaLoadingGlobal.destroy();
                    ModeList.reloadTable();

                })


            }else{
                message.error("只能选一个", 3);
            }


        }

        newProps.buttons.push(<span><Button type="primary"  onClick = {()=> {submit(); }}> 推送档案系统 < /Button>< /span >)
        return newProps;
    }
})
