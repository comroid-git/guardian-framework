if (isWindows === undefined)
    isWindows = false;
if (socketToken === undefined)
    socketToken = '';
let ws = undefined;

function sendLocalEvent(type, data) {
    let e = new CustomEvent(type, {
        detail: data
    });
    console.debug('dispatching local event:', e)
    document.dispatchEvent(e);
}

function populateTag(data, tag, names, index) {
    if (data === undefined)
        tag.innerText = 'undefined';
    if (names.length - 1 > index)
        return populateTag(data[names[index]], tag, names, index + 1)
    tag.innerText = data[names[index]]
}

function changePanelEvent(event, dom) {
    console.debug('looking for received scripts...');
    new DOMParser().parseFromString(dom, "text/html")
        .querySelectorAll('script[type="application/javascript"]')
        .forEach(dom => {
            let rcvscr = dom.textContent;
            //console.debug('evaluating received script:', rcvscr);
            eval(rcvscr)
        })
    document.getElementById('content').innerHTML = dom
    sendLocalEvent('frameReady');
}

function injectionEvent(event, data) {
    document.querySelectorAll('[inject]')
        .forEach(dom => {
            let path = dom.getAttribute('inject').split('.');
            populateTag(data, dom, path, 0);
        });
}

function handleMessage(json) {
    console.debug('incoming event:', json);

    if (json.startsWith('hello'))
        return;

    let event = JSON.parse(json);
    const type = event['type'];
    const data = event['data'];

    switch (type) {
        case 'inject':
            injectionEvent(event, data);
            break;
        case 'changePanel':
            changePanelEvent(event, data);
            break;
    }

    sendLocalEvent(type, data);
}

function sendCommand(command) {
    sendLocalEvent(command);
    ws.send(JSON.stringify({
        'type': command
    }));
}

function actionChangePanel(target) {
    const isWsReady = ws.readyState === WebSocket.OPEN;
    let url = '/' + target;
    if (isWsReady) {
        console.debug('socket is ready; changing content via socket;', url)
        ws.send(JSON.stringify({
            'type': 'webkit/changePanel',
            'data': {
                'target': target
            }
        }));
        window.history.pushState("", "", url);
    } else {
        console.debug('socket is not ready; changing content via location;', url)
        window.location.href = url;
    }
}

function initAPI() {
    console.debug('loading socket');

    ws = new WebSocket(isWindows ? "ws://localhost:42001" : `wss://${window.location.host}/websocket`);
    ws.onopen = (msg) => {
        console.debug("open ", msg);
        ws.send("hello server; i'm " + (socketToken === undefined || socketToken === '' ? 'unknown' : socketToken));
        sendLocalEvent('frameReady');
    }
    ws.onmessage = (msg) => handleMessage(msg.data);
    ws.onerror = (msg) => console.debug("error in websocket", msg);
    ws.onclose = (msg) => console.debug("websocket closed", msg);
}

function disconnectAPI() {
    ws?.close(1000);
}
