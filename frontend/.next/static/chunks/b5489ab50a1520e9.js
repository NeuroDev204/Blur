(globalThis.TURBOPACK||(globalThis.TURBOPACK=[])).push(["object"==typeof document?document.currentScript:void 0,14,e=>{"use strict";e.s(["API_BASE_URL",0,"http://localhost:8888/api","SOCKET_URL",0,"http://localhost:8099"])},18566,(e,t,r)=>{t.exports=e.r(76562)},5766,e=>{"use strict";let t,r;var n,o=e.i(71645);let a={data:""},i=/(?:([\u0080-\uFFFF\w-%@]+) *:? *([^{;]+?);|([^;}{]*?) *{)|(}\s*)/g,s=/\/\*[^]*?\*\/|  +/g,l=/\n+/g,c=(e,t)=>{let r="",n="",o="";for(let a in e){let i=e[a];"@"==a[0]?"i"==a[1]?r=a+" "+i+";":n+="f"==a[1]?c(i,a):a+"{"+c(i,"k"==a[1]?"":t)+"}":"object"==typeof i?n+=c(i,t?t.replace(/([^,])+/g,e=>a.replace(/([^,]*:\S+\([^)]*\))|([^,])+/g,t=>/&/.test(t)?t.replace(/&/g,e):e?e+" "+t:t)):a):null!=i&&(a=/^--/.test(a)?a:a.replace(/[A-Z]/g,"-$&").toLowerCase(),o+=c.p?c.p(a,i):a+":"+i+";")}return r+(t&&o?t+"{"+o+"}":o)+n},d={},u=e=>{if("object"==typeof e){let t="";for(let r in e)t+=r+u(e[r]);return t}return e};function m(e){let t,r,n=this||{},o=e.call?e(n.p):e;return((e,t,r,n,o)=>{var a;let m=u(e),f=d[m]||(d[m]=(e=>{let t=0,r=11;for(;t<e.length;)r=101*r+e.charCodeAt(t++)>>>0;return"go"+r})(m));if(!d[f]){let t=m!==e?e:(e=>{let t,r,n=[{}];for(;t=i.exec(e.replace(s,""));)t[4]?n.shift():t[3]?(r=t[3].replace(l," ").trim(),n.unshift(n[0][r]=n[0][r]||{})):n[0][t[1]]=t[2].replace(l," ").trim();return n[0]})(e);d[f]=c(o?{["@keyframes "+f]:t}:t,r?"":"."+f)}let p=r&&d.g?d.g:null;return r&&(d.g=d[f]),a=d[f],p?t.data=t.data.replace(p,a):-1===t.data.indexOf(a)&&(t.data=n?a+t.data:t.data+a),f})(o.unshift?o.raw?(t=[].slice.call(arguments,1),r=n.p,o.reduce((e,n,o)=>{let a=t[o];if(a&&a.call){let e=a(r),t=e&&e.props&&e.props.className||/^go/.test(e)&&e;a=t?"."+t:e&&"object"==typeof e?e.props?"":c(e,""):!1===e?"":e}return e+n+(null==a?"":a)},"")):o.reduce((e,t)=>Object.assign(e,t&&t.call?t(n.p):t),{}):o,(e=>{if("object"==typeof window){let t=(e?e.querySelector("#_goober"):window._goober)||Object.assign(document.createElement("style"),{innerHTML:" ",id:"_goober"});return t.nonce=window.__nonce__,t.parentNode||(e||document.head).appendChild(t),t.firstChild}return e||a})(n.target),n.g,n.o,n.k)}m.bind({g:1});let f,p,g,h=m.bind({k:1});function b(e,t){let r=this||{};return function(){let n=arguments;function o(a,i){let s=Object.assign({},a),l=s.className||o.className;r.p=Object.assign({theme:p&&p()},s),r.o=/ *go\d+/.test(l),s.className=m.apply(r,n)+(l?" "+l:""),t&&(s.ref=i);let c=e;return e[0]&&(c=s.as||e,delete s.as),g&&c[0]&&g(s),f(c,s)}return t?t(o):o}}var y=(e,t)=>"function"==typeof e?e(t):e,v=(t=0,()=>(++t).toString()),x=()=>{if(void 0===r&&"u">typeof window){let e=matchMedia("(prefers-reduced-motion: reduce)");r=!e||e.matches}return r},w="default",k=(e,t)=>{let{toastLimit:r}=e.settings;switch(t.type){case 0:return{...e,toasts:[t.toast,...e.toasts].slice(0,r)};case 1:return{...e,toasts:e.toasts.map(e=>e.id===t.toast.id?{...e,...t.toast}:e)};case 2:let{toast:n}=t;return k(e,{type:+!!e.toasts.find(e=>e.id===n.id),toast:n});case 3:let{toastId:o}=t;return{...e,toasts:e.toasts.map(e=>e.id===o||void 0===o?{...e,dismissed:!0,visible:!1}:e)};case 4:return void 0===t.toastId?{...e,toasts:[]}:{...e,toasts:e.toasts.filter(e=>e.id!==t.toastId)};case 5:return{...e,pausedAt:t.time};case 6:let a=t.time-(e.pausedAt||0);return{...e,pausedAt:void 0,toasts:e.toasts.map(e=>({...e,pauseDuration:e.pauseDuration+a}))}}},C=[],N={toasts:[],pausedAt:void 0,settings:{toastLimit:20}},j={},E=(e,t=w)=>{j[t]=k(j[t]||N,e),C.forEach(([e,r])=>{e===t&&r(j[t])})},I=e=>Object.keys(j).forEach(t=>E(e,t)),A=(e=w)=>t=>{E(t,e)},R={blank:4e3,error:4e3,success:2e3,loading:1/0,custom:4e3},S=e=>(t,r)=>{let n,o=((e,t="blank",r)=>({createdAt:Date.now(),visible:!0,dismissed:!1,type:t,ariaProps:{role:"status","aria-live":"polite"},message:e,pauseDuration:0,...r,id:(null==r?void 0:r.id)||v()}))(t,e,r);return A(o.toasterId||(n=o.id,Object.keys(j).find(e=>j[e].toasts.some(e=>e.id===n))))({type:2,toast:o}),o.id},_=(e,t)=>S("blank")(e,t);_.error=S("error"),_.success=S("success"),_.loading=S("loading"),_.custom=S("custom"),_.dismiss=(e,t)=>{let r={type:3,toastId:e};t?A(t)(r):I(r)},_.dismissAll=e=>_.dismiss(void 0,e),_.remove=(e,t)=>{let r={type:4,toastId:e};t?A(t)(r):I(r)},_.removeAll=e=>_.remove(void 0,e),_.promise=(e,t,r)=>{let n=_.loading(t.loading,{...r,...null==r?void 0:r.loading});return"function"==typeof e&&(e=e()),e.then(e=>{let o=t.success?y(t.success,e):void 0;return o?_.success(o,{id:n,...r,...null==r?void 0:r.success}):_.dismiss(n),e}).catch(e=>{let o=t.error?y(t.error,e):void 0;o?_.error(o,{id:n,...r,...null==r?void 0:r.error}):_.dismiss(n)}),e};var $=1e3,T=h`
from {
  transform: scale(0) rotate(45deg);
	opacity: 0;
}
to {
 transform: scale(1) rotate(45deg);
  opacity: 1;
}`,O=h`
from {
  transform: scale(0);
  opacity: 0;
}
to {
  transform: scale(1);
  opacity: 1;
}`,D=h`
from {
  transform: scale(0) rotate(90deg);
	opacity: 0;
}
to {
  transform: scale(1) rotate(90deg);
	opacity: 1;
}`,P=b("div")`
  width: 20px;
  opacity: 0;
  height: 20px;
  border-radius: 10px;
  background: ${e=>e.primary||"#ff4b4b"};
  position: relative;
  transform: rotate(45deg);

  animation: ${T} 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)
    forwards;
  animation-delay: 100ms;

  &:after,
  &:before {
    content: '';
    animation: ${O} 0.15s ease-out forwards;
    animation-delay: 150ms;
    position: absolute;
    border-radius: 3px;
    opacity: 0;
    background: ${e=>e.secondary||"#fff"};
    bottom: 9px;
    left: 4px;
    height: 2px;
    width: 12px;
  }

  &:before {
    animation: ${D} 0.15s ease-out forwards;
    animation-delay: 180ms;
    transform: rotate(90deg);
  }
`,L=h`
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
`,M=b("div")`
  width: 12px;
  height: 12px;
  box-sizing: border-box;
  border: 2px solid;
  border-radius: 100%;
  border-color: ${e=>e.secondary||"#e0e0e0"};
  border-right-color: ${e=>e.primary||"#616161"};
  animation: ${L} 1s linear infinite;
`,U=h`
from {
  transform: scale(0) rotate(45deg);
	opacity: 0;
}
to {
  transform: scale(1) rotate(45deg);
	opacity: 1;
}`,z=h`
0% {
	height: 0;
	width: 0;
	opacity: 0;
}
40% {
  height: 0;
	width: 6px;
	opacity: 1;
}
100% {
  opacity: 1;
  height: 10px;
}`,F=b("div")`
  width: 20px;
  opacity: 0;
  height: 20px;
  border-radius: 10px;
  background: ${e=>e.primary||"#61d345"};
  position: relative;
  transform: rotate(45deg);

  animation: ${U} 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)
    forwards;
  animation-delay: 100ms;
  &:after {
    content: '';
    box-sizing: border-box;
    animation: ${z} 0.2s ease-out forwards;
    opacity: 0;
    animation-delay: 200ms;
    position: absolute;
    border-right: 2px solid;
    border-bottom: 2px solid;
    border-color: ${e=>e.secondary||"#fff"};
    bottom: 6px;
    left: 6px;
    height: 10px;
    width: 6px;
  }
`,W=b("div")`
  position: absolute;
`,B=b("div")`
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 20px;
  min-height: 20px;
`,K=h`
from {
  transform: scale(0.6);
  opacity: 0.4;
}
to {
  transform: scale(1);
  opacity: 1;
}`,H=b("div")`
  position: relative;
  transform: scale(0.6);
  opacity: 0.4;
  min-width: 20px;
  animation: ${K} 0.3s 0.12s cubic-bezier(0.175, 0.885, 0.32, 1.275)
    forwards;
`,q=({toast:e})=>{let{icon:t,type:r,iconTheme:n}=e;return void 0!==t?"string"==typeof t?o.createElement(H,null,t):t:"blank"===r?null:o.createElement(B,null,o.createElement(M,{...n}),"loading"!==r&&o.createElement(W,null,"error"===r?o.createElement(P,{...n}):o.createElement(F,{...n})))},V=b("div")`
  display: flex;
  align-items: center;
  background: #fff;
  color: #363636;
  line-height: 1.3;
  will-change: transform;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1), 0 3px 3px rgba(0, 0, 0, 0.05);
  max-width: 350px;
  pointer-events: auto;
  padding: 8px 10px;
  border-radius: 8px;
`,Z=b("div")`
  display: flex;
  justify-content: center;
  margin: 4px 10px;
  color: inherit;
  flex: 1 1 auto;
  white-space: pre-line;
`,X=o.memo(({toast:e,position:t,style:r,children:n})=>{let a=e.height?((e,t)=>{let r=e.includes("top")?1:-1,[n,o]=x()?["0%{opacity:0;} 100%{opacity:1;}","0%{opacity:1;} 100%{opacity:0;}"]:[`
0% {transform: translate3d(0,${-200*r}%,0) scale(.6); opacity:.5;}
100% {transform: translate3d(0,0,0) scale(1); opacity:1;}
`,`
0% {transform: translate3d(0,0,-1px) scale(1); opacity:1;}
100% {transform: translate3d(0,${-150*r}%,-1px) scale(.6); opacity:0;}
`];return{animation:t?`${h(n)} 0.35s cubic-bezier(.21,1.02,.73,1) forwards`:`${h(o)} 0.4s forwards cubic-bezier(.06,.71,.55,1)`}})(e.position||t||"top-center",e.visible):{opacity:0},i=o.createElement(q,{toast:e}),s=o.createElement(Z,{...e.ariaProps},y(e.message,e));return o.createElement(V,{className:e.className,style:{...a,...r,...e.style}},"function"==typeof n?n({icon:i,message:s}):o.createElement(o.Fragment,null,i,s))});n=o.createElement,c.p=void 0,f=n,p=void 0,g=void 0;var J=({id:e,className:t,style:r,onHeightUpdate:n,children:a})=>{let i=o.useCallback(t=>{if(t){let r=()=>{n(e,t.getBoundingClientRect().height)};r(),new MutationObserver(r).observe(t,{subtree:!0,childList:!0,characterData:!0})}},[e,n]);return o.createElement("div",{ref:i,className:t,style:r},a)},Y=m`
  z-index: 9999;
  > * {
    pointer-events: auto;
  }
`,G=({reverseOrder:e,position:t="top-center",toastOptions:r,gutter:n,children:a,toasterId:i,containerStyle:s,containerClassName:l})=>{let{toasts:c,handlers:d}=((e,t="default")=>{let{toasts:r,pausedAt:n}=((e={},t=w)=>{let[r,n]=(0,o.useState)(j[t]||N),a=(0,o.useRef)(j[t]);(0,o.useEffect)(()=>(a.current!==j[t]&&n(j[t]),C.push([t,n]),()=>{let e=C.findIndex(([e])=>e===t);e>-1&&C.splice(e,1)}),[t]);let i=r.toasts.map(t=>{var r,n,o;return{...e,...e[t.type],...t,removeDelay:t.removeDelay||(null==(r=e[t.type])?void 0:r.removeDelay)||(null==e?void 0:e.removeDelay),duration:t.duration||(null==(n=e[t.type])?void 0:n.duration)||(null==e?void 0:e.duration)||R[t.type],style:{...e.style,...null==(o=e[t.type])?void 0:o.style,...t.style}}});return{...r,toasts:i}})(e,t),a=(0,o.useRef)(new Map).current,i=(0,o.useCallback)((e,t=$)=>{if(a.has(e))return;let r=setTimeout(()=>{a.delete(e),s({type:4,toastId:e})},t);a.set(e,r)},[]);(0,o.useEffect)(()=>{if(n)return;let e=Date.now(),o=r.map(r=>{if(r.duration===1/0)return;let n=(r.duration||0)+r.pauseDuration-(e-r.createdAt);if(n<0){r.visible&&_.dismiss(r.id);return}return setTimeout(()=>_.dismiss(r.id,t),n)});return()=>{o.forEach(e=>e&&clearTimeout(e))}},[r,n,t]);let s=(0,o.useCallback)(A(t),[t]),l=(0,o.useCallback)(()=>{s({type:5,time:Date.now()})},[s]),c=(0,o.useCallback)((e,t)=>{s({type:1,toast:{id:e,height:t}})},[s]),d=(0,o.useCallback)(()=>{n&&s({type:6,time:Date.now()})},[n,s]),u=(0,o.useCallback)((e,t)=>{let{reverseOrder:n=!1,gutter:o=8,defaultPosition:a}=t||{},i=r.filter(t=>(t.position||a)===(e.position||a)&&t.height),s=i.findIndex(t=>t.id===e.id),l=i.filter((e,t)=>t<s&&e.visible).length;return i.filter(e=>e.visible).slice(...n?[l+1]:[0,l]).reduce((e,t)=>e+(t.height||0)+o,0)},[r]);return(0,o.useEffect)(()=>{r.forEach(e=>{if(e.dismissed)i(e.id,e.removeDelay);else{let t=a.get(e.id);t&&(clearTimeout(t),a.delete(e.id))}})},[r,i]),{toasts:r,handlers:{updateHeight:c,startPause:l,endPause:d,calculateOffset:u}}})(r,i);return o.createElement("div",{"data-rht-toaster":i||"",style:{position:"fixed",zIndex:9999,top:16,left:16,right:16,bottom:16,pointerEvents:"none",...s},className:l,onMouseEnter:d.startPause,onMouseLeave:d.endPause},c.map(r=>{let i,s,l=r.position||t,c=d.calculateOffset(r,{reverseOrder:e,gutter:n,defaultPosition:t}),u=(i=l.includes("top"),s=l.includes("center")?{justifyContent:"center"}:l.includes("right")?{justifyContent:"flex-end"}:{},{left:0,right:0,display:"flex",position:"absolute",transition:x()?void 0:"all 230ms cubic-bezier(.21,1.02,.73,1)",transform:`translateY(${c*(i?1:-1)}px)`,...i?{top:0}:{bottom:0},...s});return o.createElement(J,{id:r.id,key:r.id,onHeightUpdate:d.updateHeight,className:r.visible?Y:"",style:u},"custom"===r.type?y(r.message,r):a?a(r):o.createElement(X,{toast:r,position:l}))}))};e.s(["Toaster",()=>G,"default",()=>_],5766)},42094,e=>{"use strict";let t=()=>localStorage.getItem("token");e.s(["getToken",0,t,"getUserId",0,()=>{let e=t();if(!e)return null;try{let t=e.split(".")[1],r=JSON.parse(atob(t));return r.sub||r.userId||r.user_id||r.id||null}catch(e){return console.error("Cannot decode token:",e),null}},"isAuthenticated",0,()=>!!t(),"removeToken",0,()=>{localStorage.removeItem("token")},"setToken",0,e=>{localStorage.setItem("token",e)}])},21151,e=>{"use strict";var t=e.i(43476),r=e.i(71645),n=e.i(14),o=e.i(42094);let a=(0,r.createContext)(null);e.s(["SocketProvider",0,({children:e})=>{let i=(0,r.useRef)(null),[s,l]=(0,r.useState)(!1),[c,d]=(0,r.useState)(""),u=(0,r.useRef)({onMessageSent:null,onMessageReceived:null}),m=(0,r.useRef)({onCallInitiated:null,onIncomingCall:null,onCallAnswered:null,onCallRejected:null,onCallEnded:null,onCallFailed:null,onWebRTCOffer:null,onWebRTCAnswer:null,onICECandidate:null}),f=(0,r.useCallback)(e=>{u.current={...u.current,...e}},[]),p=(0,r.useCallback)(e=>{Object.keys(e).some(t=>m.current[t]!==e[t])&&(m.current={...m.current,...e})},[]);(0,r.useEffect)(()=>{let e=(0,o.getToken)();if(!e)return;let t=document.createElement("script");return t.src="https://cdn.socket.io/4.5.4/socket.io.min.js",t.async=!0,t.onload=()=>{if(i.current)return;let t=window.io(n.SOCKET_URL,{query:{token:e},autoConnect:!0,reconnection:!0,reconnectionDelay:1e3,reconnectionAttempts:10,timeout:2e4,transports:["websocket","polling"]});i.current=t,t.on("connect",()=>{l(!0),d("")}),t.on("disconnect",()=>{l(!1)}),t.on("connect_error",()=>{d("Không thể kết nối socket"),l(!1)}),t.on("reconnect_attempt",()=>{}),t.on("reconnect",()=>{l(!0),d("")}),t.on("connected",()=>{}),t.on("message_sent",e=>{u.current.onMessageSent&&u.current.onMessageSent(e)}),t.on("message_received",e=>{u.current.onMessageReceived&&u.current.onMessageReceived(e)}),t.on("user_typing",()=>{}),t.on("message_error",e=>{d(e?.message||"Lỗi khi gửi tin nhắn")}),t.on("auth_error",()=>{d("Lỗi xác thực. Vui lòng đăng nhập lại.")}),t.on("call:initiated",e=>{console.log("✅ Received call:initiated event:",e),m.current.onCallInitiated&&m.current.onCallInitiated(e)}),t.on("call:incoming",e=>{console.log("📞 Received call:incoming event:",e),m.current.onIncomingCall&&m.current.onIncomingCall(e)}),t.on("call:answered",e=>{console.log("✅ Received call:answered event:",e),m.current.onCallAnswered&&m.current.onCallAnswered(e)}),t.on("call:rejected",e=>{m.current.onCallRejected&&m.current.onCallRejected(e)}),t.on("call:ended",e=>{m.current.onCallEnded&&m.current.onCallEnded(e)}),t.on("call:failed",e=>{m.current.onCallFailed&&m.current.onCallFailed(e)}),t.on("webrtc:offer",e=>{m.current.onWebRTCOffer&&m.current.onWebRTCOffer(e)}),t.on("webrtc:answer",e=>{m.current.onWebRTCAnswer&&m.current.onWebRTCAnswer(e)}),t.on("webrtc:ice-candidate",e=>{m.current.onICECandidate&&m.current.onICECandidate(e)})},t.onerror=()=>{d("Không thể tải thư viện Socket.IO")},document.head.appendChild(t),()=>{i.current&&(i.current.off("connect"),i.current.off("disconnect"),i.current.off("connect_error"),i.current.off("reconnect_attempt"),i.current.off("reconnect"),i.current.off("connected"),i.current.off("message_sent"),i.current.off("message_received"),i.current.off("user_typing"),i.current.off("message_error"),i.current.off("auth_error"),i.current.off("call:initiated"),i.current.off("call:incoming"),i.current.off("call:answered"),i.current.off("call:rejected"),i.current.off("call:ended"),i.current.off("call:failed"),i.current.off("webrtc:offer"),i.current.off("webrtc:answer"),i.current.off("webrtc:ice-candidate"),i.current.disconnect(),i.current=null),t.parentNode&&t.parentNode.removeChild(t)}},[]);let g=(0,r.useCallback)(e=>{if(!i.current||!s)return!1;try{return i.current.emit("send_message",e),!0}catch{return!1}},[s]),h=(0,r.useCallback)((e,t)=>{if(i.current&&s)try{i.current.emit("typing",{conversationId:e,isTyping:t})}catch{}},[s]),b=(0,r.useCallback)(e=>{if(!i.current||!s)return console.error("❌ Socket not connected or socketRef is null"),!1;try{return console.log("📡 Emitting call:initiate event:",e),i.current.emit("call:initiate",e),!0}catch(e){return console.error("❌ Error emitting call:initiate:",e),!1}},[s]),y=(0,r.useCallback)(e=>{if(!i.current||!s)return!1;try{return i.current.emit("call:answer",{callId:e}),!0}catch{return!1}},[s]),v=(0,r.useCallback)(e=>{if(!i.current||!s)return!1;try{return i.current.emit("call:reject",{callId:e}),!0}catch{return!1}},[s]),x=(0,r.useCallback)(e=>{if(!i.current||!s)return!1;try{return i.current.emit("call:end",{callId:e}),!0}catch{return!1}},[s]),w=(0,r.useCallback)((e,t)=>{if(!i.current||!s)return!1;try{return i.current.emit("webrtc:offer",{to:e,offer:t}),!0}catch{return!1}},[s]),k=(0,r.useCallback)((e,t)=>{if(!i.current||!s)return!1;try{return i.current.emit("webrtc:answer",{to:e,answer:t}),!0}catch{return!1}},[s]),C=(0,r.useCallback)((e,t)=>{if(!i.current||!s)return!1;try{return i.current.emit("webrtc:ice-candidate",{to:e,candidate:t}),!0}catch{return!1}},[s]),N=(0,r.useMemo)(()=>({socket:i.current,isConnected:s,error:c,sendMessage:g,sendTypingIndicator:h,registerMessageCallbacks:f,initiateCall:b,answerCall:y,rejectCall:v,endCall:x,sendWebRTCOffer:w,sendWebRTCAnswer:k,sendICECandidate:C,registerCallCallbacks:p}),[s,c,g,h,f,b,y,v,x,w,k,C,p]);return(0,t.jsx)(a.Provider,{value:N,children:e})},"useSocket",0,()=>{let e=(0,r.useContext)(a);if(!e)throw Error("useSocket must be used within SocketProvider");return e}])},37727,75254,e=>{"use strict";var t=e.i(71645);let r=e=>{let t=e.replace(/^([A-Z])|[\s-_]+(\w)/g,(e,t,r)=>r?r.toUpperCase():t.toLowerCase());return t.charAt(0).toUpperCase()+t.slice(1)},n=(...e)=>e.filter((e,t,r)=>!!e&&""!==e.trim()&&r.indexOf(e)===t).join(" ").trim();var o={xmlns:"http://www.w3.org/2000/svg",width:24,height:24,viewBox:"0 0 24 24",fill:"none",stroke:"currentColor",strokeWidth:2,strokeLinecap:"round",strokeLinejoin:"round"};let a=(0,t.forwardRef)(({color:e="currentColor",size:r=24,strokeWidth:a=2,absoluteStrokeWidth:i,className:s="",children:l,iconNode:c,...d},u)=>(0,t.createElement)("svg",{ref:u,...o,width:r,height:r,stroke:e,strokeWidth:i?24*Number(a)/Number(r):a,className:n("lucide",s),...!l&&!(e=>{for(let t in e)if(t.startsWith("aria-")||"role"===t||"title"===t)return!0})(d)&&{"aria-hidden":"true"},...d},[...c.map(([e,r])=>(0,t.createElement)(e,r)),...Array.isArray(l)?l:[l]])),i=(e,o)=>{let i=(0,t.forwardRef)(({className:i,...s},l)=>(0,t.createElement)(a,{ref:l,iconNode:o,className:n(`lucide-${r(e).replace(/([a-z0-9])([A-Z])/g,"$1-$2").toLowerCase()}`,`lucide-${e}`,i),...s}));return i.displayName=r(e),i};e.s(["default",()=>i],75254);let s=i("x",[["path",{d:"M18 6 6 18",key:"1bl5f8"}],["path",{d:"m6 6 12 12",key:"d8bk6v"}]]);e.s(["X",()=>s],37727)},67051,e=>{"use strict";var t=e.i(43476),r=e.i(71645),n=e.i(37727),o=e.i(18566);let a=(0,r.createContext)(null),i=async()=>"Notification"in window?"granted"===Notification.permission||"denied"!==Notification.permission&&"granted"===await Notification.requestPermission():(console.log("Browser không hỗ trợ notification"),!1),s=({notification:e,onClose:o,onClick:a})=>{let i=(0,r.useMemo)(()=>{if(e.senderName&&"Unknown User"!==e.senderName)return e.senderName;let t=e.senderFirstName||"",r=e.senderLastName||"";return`${t} ${r}`.trim()||"Unknown User"},[e.senderFirstName,e.senderLastName,e.senderName]);return(0,t.jsx)("div",{onClick:()=>{a(e),o(e.id)},className:"w-[320px] bg-white rounded-xl shadow-lg border border-sky-100 p-3 cursor-pointer hover:shadow-xl hover:-translate-y-0.5 transition-all duration-200",children:(0,t.jsxs)("div",{className:"flex items-start gap-3",children:[(0,t.jsx)("img",{src:e.avatar||"https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png",alt:i,className:"w-10 h-10 rounded-full object-cover border border-sky-200 flex-shrink-0"}),(0,t.jsxs)("div",{className:"flex-1 min-w-0",children:[(0,t.jsxs)("p",{className:"text-sm text-gray-800 leading-snug",children:[(0,t.jsx)("span",{className:"font-semibold mr-1",children:i}),e.message||"đã gửi một thông báo cho bạn."]}),(0,t.jsx)("p",{className:"text-xs text-gray-400 mt-1",children:l(e.createdDate)}),e.postId&&(0,t.jsx)("p",{className:"text-xs text-sky-600 font-medium mt-1",children:"Click để xem →"})]}),(0,t.jsx)("button",{onClick:t=>{t.stopPropagation(),o(e.id)},className:"ml-1 text-gray-400 hover:text-gray-600",children:(0,t.jsx)(n.X,{size:14})})]})})},l=e=>{if(!e)return"Vừa xong";let t=new Date(e),r=Math.floor((new Date().getTime()-t.getTime())/1e3);return r<60?"Vừa xong":r<3600?`${Math.floor(r/60)} ph\xfat trước`:r<86400?`${Math.floor(r/3600)} giờ trước`:t.toLocaleTimeString("vi-VN",{hour:"2-digit",minute:"2-digit",day:"2-digit",month:"2-digit"})};e.s(["NotificationProvider",0,({children:e})=>{let[n,i]=(0,r.useState)([]),[l,c]=(0,r.useState)("toast"),[d,u]=(0,r.useState)(0),m=(0,o.useRouter)(),f=(0,r.useCallback)(e=>{console.log("🔔 Adding notification:",e),i(t=>{if(!e?.id)return console.warn("⚠️ Notification missing ID"),t;if(t.some(t=>t.id===e.id))return console.log("⚠️ Notification already exists:",e.id),t;let r=[e.senderFirstName,e.senderLastName].filter(Boolean).join(" ")||e.senderName||"Unknown User",n={id:e.id,senderFirstName:e.senderFirstName,senderLastName:e.senderLastName,senderName:r,avatar:e.avatar||e.senderImageUrl,message:e.content||e.message,createdDate:e.createdDate||e.timestamp||new Date().toISOString(),type:e.type||"general",seen:e.seen??e.read??!1,postId:e.postId,senderId:e.senderId};return(()=>{try{new Audio("/notification.mp3").play().catch(()=>{})}catch{}})(),"undefined"!=typeof document&&document.hidden&&(e=>{if("Notification"in window&&"granted"===Notification.permission){let t=new Notification(e.senderName,{body:e.message,icon:e.avatar});setTimeout(()=>t.close(),5e3)}})(n),u(e=>e+1),console.log("✅ Notification added to state"),[n,...t]})},[]),p=(0,r.useCallback)(e=>{i(e)},[]),g=(0,r.useCallback)(e=>{i(t=>t.filter(t=>t.id!==e))},[]),h=(0,r.useCallback)(e=>{e.onClick&&e.onClick(e),e.postId&&m.push(`/post/${e.postId}`),g(e.id)},[m,g]),b=(0,r.useMemo)(()=>({notifications:n,addNotification:f,removeNotification:g,setNotificationsList:p,setMode:c,mode:l,notificationCounter:d}),[n,f,g,p,l,d]);return(0,t.jsxs)(a.Provider,{value:b,children:[e,(0,t.jsx)("div",{className:"fixed top-4 right-4 z-[9999] space-y-3 max-h-screen overflow-y-auto pointer-events-none",children:(0,t.jsx)("div",{className:"pointer-events-auto",children:"toast"===l&&n.map(e=>(0,t.jsx)(s,{notification:e,onClose:g,onClick:h},e.id))})})]})},"requestNotificationPermission",0,i,"useNotification",0,()=>{let e=(0,r.useContext)(a);if(!e)throw Error("useNotification must be used within NotificationProvider");return e}])},41403,e=>{e.v(t=>Promise.all(["static/chunks/e7c96e2d7e87702f.js"].map(t=>e.l(t))).then(()=>t(19707)))},97274,e=>{e.v(t=>Promise.all(["static/chunks/1167514565796fe4.js"].map(t=>e.l(t))).then(()=>t(31592)))}]);