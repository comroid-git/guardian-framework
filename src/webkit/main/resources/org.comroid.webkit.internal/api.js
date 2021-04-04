if (isDebug === undefined)
    isDebug = false;

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

    var ws = new WebSocket(isDebug ? "ws://localhost:42001" : `ws://${window.location.host}/websocket`);
    ws.onmessage = (msg) => handleMessage(msg.data);
}
