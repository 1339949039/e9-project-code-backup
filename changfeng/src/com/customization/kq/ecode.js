let enable = true;
let isRun = false; //控制执行次数
const runScript = (newProps) => { //代码块钩子，类似放在代码块中或者jquery.ready
    isRun = true; //确保只执行一次
}
const titleAttr = ["workdays", "workmins", "attendancedays", "beLate", "beLateMins", "graveBeLate", "graveBeLateMins", "leaveEearly", "leaveEarlyMins", "graveLeaveEarly", "graveLeaveEarlyMins", "absenteeism", "absenteeismMins", "forgotCheck", "leave", "overtime", "businessLeave", "officialBusiness"];
//PC端代码块
//利用组件复写作为代码块执行钩子，这种方案可以支持覆盖到所有流程，也可以判断到指定流程指定节点
ecodeSDK.overwritePropsFnQueueMapSet('WeaTable', {
    fn: (newProps) => {
        if (!enable) return; //开关打开
        const { hash } = window.location;
        if (!hash.startsWith('#/main/attendance/report/daily')) return; //判断页面地址
        if (!ecCom.WeaTools.Base64) return; //完整组件库加载完成
        //if(!WfForm) return ; //表单sdk加载完成
        //if(isRun) return ; //执行过一次就不执行
        //runScript(newProps); //执行代码块
        newProps.columns = newProps.columns.map(obj => {
            //上班1
            if (obj.dataIndex === "signin1") {
                return { ...obj, children: mapkq(obj, 'signinstatus1') };
            }
            //下班1
            if (obj.dataIndex === "signout1") {
                return { ...obj, children: mapkq(obj, 'signoutstatus1') };
            }
            //上班2
            if (obj.dataIndex === "signin2") {
                return { ...obj, children: mapkq(obj, 'signinstatus2') };
            }
            //下班2
            if (obj.dataIndex === "signout2") {
                //signinstatus1
                return { ...obj, children: mapkq(obj, 'signoutstatus2') };
            }

            //上班3
            if (obj.dataIndex === "signin3") {
                return { ...obj, children: mapkq(obj, 'signinstatus3') };
            }
            //下班3
            if (obj.dataIndex === "signout3") {
                return { ...obj, children: mapkq(obj, 'signoutstatus3') };
            }
            //approvalSlip 审批单
            if (obj.dataIndex === "approvalSlip") {
                return { ...obj, render: (text, record, index) => {

                        if (record.style != undefined && record.style.includes('approvalSlip')) {
                            let style = { 'backgroundColor': '#ffcc99' };//鸡蛋色
                            if (record.style.includes("BRIGHT_GREEN")) {
                                style = { 'backgroundColor': 'green' }//绿色
                            }
                            if (record.style.includes("LIGHT_YELLOW")) {
                                style = { 'backgroundColor': 'yellow' }//黄色
                            }
                            if (record.style.includes("CORAL")) {
                                style = { 'backgroundColor': '#ff8080' };//红色
                            }
                            return (<div style= { style } > { text } < /div>);
                        }
                        return (<div>{ text } < /div>);
                    } };

            }
            return obj; // 不需要修改的对象直接返回原对象
        });

        return newProps;

    }
});

function mapkq(obj, sign) {
    let children = obj.children.map(child => {
        //上班1考勤
        if (child.dataIndex === sign) {
            //判断style数组是否包含
            return {...child, render: (text, record, index) => {
                    if (record.style != undefined && record.style.includes(sign)) {
                        let style = { 'backgroundColor': '#ffcc99' };//鸡蛋色
                        if (record.style.includes("BRIGHT_GREEN")) {
                            style = { 'backgroundColor': 'green' }//绿色
                        }
                        if (record.style.includes("LIGHT_YELLOW")) {
                            style = { 'backgroundColor': 'yellow' }//黄色
                        }
                        if (record.style.includes("CORAL")) {
                            style = { 'backgroundColor': '#ff8080' };//红色
                        }
                        return (<div style= { style } > { text } < /div>);
                    }
                    return (<div>{ text } < /div>);
                }
            }
        }
        return child;
    })
    return children;

}
