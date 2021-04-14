if (isDebug === undefined)
    isDebug = false;
let ws = undefined;
let cache = undefined;

function populateTag(data, tag, names, index) {
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function changePanel(dom) {
    document.getElementById('content').innerHTML = dom
}

function handleMessage(json) {
    console.debug('incoming event', json);

    let event = JSON.parse(json);
    let data = event['data'];

    switch (event['type']) {
        case 'inject':
            document.querySelectorAll('[inject]')
                .forEach(dom => {
                    let path = dom.attributes['inject'].split('.');
                    populateTag(data, dom, path, 0);
                });
            break;
        case 'changePanel':
            changePanel(data);
            break;
    }
}

function actionChangePanel(target) {
    window.history.pushState("", "", '/' + target);
    ws.send(JSON.stringify({
        'type': 'action/changePanel',
        'data': {
            'target': target
        }
    }));
}

function initAPI() {
    console.debug('loading socket');

    ws = new WebSocket(isDebug ? "ws://localhost:42001" : `ws://${window.location.host}/websocket`);
    ws.onopen = (msg) => {
        console.debug("open ", msg);
        ws.send("hello server");
    }
    ws.onmessage = (msg) => handleMessage(msg.data);
    ws.onerror = (msg) => console.debug("error in websocket", msg);
    ws.onclose = (msg) => console.debug("websocket closed", msg);
}

function disconnectAPI() {
    ws?.close(1000);
}
