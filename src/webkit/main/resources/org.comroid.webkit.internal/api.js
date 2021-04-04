if (isDebug === undefined)
    isDebug = false;

function initAPI() {
    console.debug('loading socket')
    var ws = new WebSocket(isDebug ? "ws://localhost:42001" : `ws://${window.location.host}/websocket`);

    ws.onopen = () => console.debug('opened')
    ws.onmessage = (msg) => console.debug('data received: ', msg)
}
