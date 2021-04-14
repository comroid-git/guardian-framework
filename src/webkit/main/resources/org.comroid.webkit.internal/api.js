if (isWindows === undefined)
    isWindows = false;
if (socketToken === undefined)
    socketToken = '';
let ws = undefined;

function populateTag(data, tag, names, index) {
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function changePanel(dom) {
    document.getElementById('content').innerHTML = dom
}

function handleMessage(json) {
    console.debug('incoming event:', json);

    if (json.startsWith('hello'))
        return;

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
    ws.send(JSON.stringify({
        'type': 'action/changePanel',
        'data': {
            'target': target
        }
    }));
    window.history.pushState("", "", '/' + target);
}

function initAPI() {
    console.debug('loading socket');

    ws = new WebSocket(isWindows ? "ws://localhost:42001" : `ws://${window.location.host}/websocket`);
    ws.onopen = (msg) => {
        console.debug("open ", msg);
        ws.send("hello server; i'm " + (socketToken === undefined || socketToken === '' ? 'unknown' : socketToken));
    }
    ws.onmessage = (msg) => handleMessage(msg.data);
    ws.onerror = (msg) => console.debug("error in websocket", msg);
    ws.onclose = (msg) => console.debug("websocket closed", msg);
}

function disconnectAPI() {
    ws?.close(1000);
}
