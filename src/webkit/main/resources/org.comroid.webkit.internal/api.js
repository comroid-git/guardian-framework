if (isDebug === undefined)
    isDebug = false;
let ws = undefined;

function initAPI() {
    console.debug('loading socket')

    function handleMessage(data) {
        if (data.indexOf(`id:`) !== 0) {
            console.debug('Invalid data received', data);
            return;
        }

        let idEnd = data.indexOf(';');
        let id = data.substr(3, idEnd - 3);

        document.getElementById(id).innerHTML = data.substr(idEnd + 1)
    }

    ws = new WebSocket(isDebug ? "ws://localhost:42001" : `ws://${window.location.host}/websocket`);
    ws.send("hello server")
    ws.onmessage = (msg) => handleMessage(msg.data);
}

function disconnectAPI() {
    ws?.close(1000)
}
