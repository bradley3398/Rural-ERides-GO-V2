package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.ByteArrayInputStream

/**
 * Self-contained Chromium WebGL 2.0 glTF/GLB PBR Renderer for PEV 3D Labs.
 * Loads the live procedural binary .glb rig (with embedded normal, metallic-roughness,
 * and albedo PNGs + KHR_materials_clearcoat) over a local intercepted memory stream
 * (zero internet or CDN dependencies, pure Chromium WebGL 2.0).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun Pev3dWebGlViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  lightingPreset: Pev3dLightingPreset,
  yawDeg: Float,
  pitchDeg: Float,
  zoom: Float,
  explodePct: Float,
  wheelSpinDeg: Float,
  throttlePct: Float,
  lightsOn: Boolean,
  turnSignalMode: Pev3dTurnSignalMode,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  bodyRgb: FloatArray,
  caliperRgb: FloatArray,
  ambientTempC: Float = 22f,
  onBackendChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
  finish: Pev3dModelFinish = Pev3dModelFinish(
    bodyRgb = bodyRgb,
    accentRgb = accentRgb,
    caliperRgb = caliperRgb,
  ),
) {
  val webViewRef = remember { arrayOfNulls<WebView>(1) }
  var pageLoaded by remember { mutableStateOf(false) }
  var webGlFailed by remember { mutableStateOf(false) }

  if (webGlFailed) {
    Pev3dComposeShadedViewport(
      archetype = archetype,
      renderMode = renderMode,
      lightingPreset = lightingPreset,
      yawDeg = yawDeg,
      pitchDeg = pitchDeg,
      zoom = zoom,
      explodePct = explodePct,
      wheelSpinDeg = wheelSpinDeg,
      throttlePct = throttlePct,
      lightsOn = lightsOn,
      turnSignalMode = turnSignalMode,
      selectedPartKey = selectedPartKey,
      accentRgb = accentRgb,
      bodyRgb = bodyRgb,
      caliperRgb = caliperRgb,
      ambientTempC = ambientTempC,
      modifier = modifier,
      finish = finish,
    )
    return
  }

  val builtGlb = remember(archetype, finish, lightsOn) {
    Pev3dGltfBuilder.buildVehicleGlb(
      archetype = archetype,
      finish = finish,
      lightsOn = lightsOn,
    )
  }
  val glbHolder = remember { arrayOfNulls<ByteArray>(1) }
  glbHolder[0] = builtGlb.glbBytes

  var glbVersion by remember { mutableIntStateOf(0) }
  val rigMetaJson = remember(builtGlb) {
    val axles = JSONObject()
    builtGlb.wheelAxleCenters.forEach { (k, v) ->
      axles.put(k, org.json.JSONArray().put(v.x.toDouble()).put(v.y.toDouble()).put(v.z.toDouble()))
    }
    val exps = JSONObject()
    builtGlb.nodeExplodeDirs.forEach { (k, v) ->
      exps.put(k, org.json.JSONArray().put(v.x.toDouble()).put(v.y.toDouble()).put(v.z.toDouble()))
    }
    JSONObject().put("axles", axles).put("explodes", exps).toString()
  }

  LaunchedEffect(builtGlb, pageLoaded) {
    if (!pageLoaded) return@LaunchedEffect
    val wv = webViewRef[0] ?: return@LaunchedEffect
    glbVersion++
    val ver = glbVersion
    val url = "https://local.ruralerides.webgl/rig_$ver.glb"
    runCatching { wv.evaluateJavascript("if(window.loadPevGlb)window.loadPevGlb('$url',$rigMetaJson);", null) }
  }

  LaunchedEffect(
    renderMode,
    lightingPreset,
    yawDeg,
    pitchDeg,
    zoom,
    explodePct,
    wheelSpinDeg,
    throttlePct,
    lightsOn,
    turnSignalMode,
    selectedPartKey,
    ambientTempC,
    pageLoaded,
  ) {
    if (!pageLoaded) return@LaunchedEffect
    val wv = webViewRef[0] ?: return@LaunchedEffect
    val js =
      "if(window.updatePev3d)window.updatePev3d('${renderMode.name}','${lightingPreset.name}',$yawDeg,$pitchDeg,$zoom,$explodePct,$wheelSpinDeg,$throttlePct,${if (lightsOn) 1 else 0},'${turnSignalMode.name}','${selectedPartKey.orEmpty()}',$ambientTempC);"
    runCatching { wv.evaluateJavascript(js, null) }
  }

  DisposableEffect(Unit) {
    onDispose {
      runCatching {
        webViewRef[0]?.let { wv ->
          wv.stopLoading()
          wv.destroy()
        }
      }
      webViewRef[0] = null
      glbHolder[0] = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      runCatching {
        onBackendChanged("WEBGL 2.0 • GLB PBR")
        WebView(ctx).apply {
          setBackgroundColor(AndroidColor.parseColor("#080C12"))
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
          }
          isHorizontalScrollBarEnabled = false
          isVerticalScrollBarEnabled = false
          setOnTouchListener { _, _ -> false }
          webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
              val url = request?.url?.toString().orEmpty()
              if (url.startsWith("https://local.ruralerides.webgl/rig_") && url.endsWith(".glb")) {
                val bytes = glbHolder[0] ?: return null
                return WebResourceResponse(
                  "model/gltf-binary",
                  null,
                  200,
                  "OK",
                  mapOf("Access-Control-Allow-Origin" to "*"),
                  ByteArrayInputStream(bytes),
                )
              }
              return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              pageLoaded = true
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
              pageLoaded = false
              webViewRef[0] = null
              webGlFailed = true
              onBackendChanged("Studio 3D PBR (Safe Fallback)")
              return true
            }
          }
          loadDataWithBaseURL("https://local.ruralerides.webgl/", WEBGL_GLB_PBR_HTML, "text/html", "UTF-8", null)
          webViewRef[0] = this
        }
      }.getOrElse {
        webGlFailed = true
        onBackendChanged("Studio 3D PBR (Safe Fallback)")
        android.view.View(ctx)
      }
    },
  )
}

private const val WEBGL_GLB_PBR_HTML = """<!DOCTYPE html>
<html><head><meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#080c12;}
canvas{width:100%;height:100%;display:block;}
</style></head><body>
<canvas id="c"></canvas>
<script>
const canvas = document.getElementById('c');
const gl = canvas.getContext('webgl2', {antialias:true, alpha:false}) || canvas.getContext('webgl', {antialias:true});
let S = {mode:'SOLID', light:'STUDIO', yaw:-28, pitch:16, zoom:1.05, explode:0, spin:0, thr:0.35, lon:1, sig:'OFF', sel:'battery', amb:22};
window.updatePev3d = (mode,light,yaw,pitch,zoom,explode,spin,thr,lon,sig,sel,amb)=>{
  S={mode,light,yaw,pitch,zoom,explode,spin,thr,lon,sig,sel,amb};
};

if (gl) {
  const vsSrc = `
  attribute vec3 aPos;
  attribute vec3 aNorm;
  attribute vec4 aTan;
  attribute vec2 aUv;
  uniform mat4 uMvp;
  uniform mat4 uModel;
  varying vec3 vWp;
  varying vec3 vN;
  varying vec3 vT;
  varying vec3 vB;
  varying vec2 vUv;
  void main(){
    vec4 wp = uModel * vec4(aPos, 1.0);
    vWp = wp.xyz;
    mat3 nm = mat3(uModel);
    vN = normalize(nm * aNorm);
    vT = normalize(nm * aTan.xyz);
    vB = cross(vN, vT) * aTan.w;
    vUv = aUv;
    gl_Position = uMvp * vec4(aPos, 1.0);
  }`;

  const fsSrc = `
  precision highp float;
  uniform sampler2D uAlbTex;
  uniform sampler2D uNrmTex;
  uniform sampler2D uMrTex;
  uniform vec3 uCam;
  uniform vec3 uKeyDir;
  uniform vec3 uKeyCol;
  uniform vec3 uFillCol;
  uniform vec3 uRimCol;
  uniform vec4 uBaseCol;
  uniform vec3 uEmissive;
  uniform float uMetallic;
  uniform float uRoughness;
  uniform float uClearcoat;
  uniform int uMode;
  uniform float uHeat;
  uniform float uSel;
  uniform float uAlpha;
  uniform float uT;
  varying vec3 vWp;
  varying vec3 vN;
  varying vec3 vT;
  varying vec3 vB;
  varying vec2 vUv;
  const float PI = 3.14159265;

  float D_GGX(float nh, float r){
    float a=r*r, a2=a*a, d=nh*nh*(a2-1.0)+1.0;
    return a2 / max(PI*d*d, 0.0001);
  }
  float G_GGX(float nx, float r){
    float k=(r+1.0)*(r+1.0)/8.0;
    return nx / max(nx*(1.0-k)+k, 0.0001);
  }
  vec3 studioEnv(vec3 R, float rough, vec3 rim){
    float sky = smoothstep(-0.25, 0.78, R.y);
    vec3 env = mix(vec3(0.03,0.04,0.05), mix(vec3(0.12,0.15,0.21), vec3(0.26,0.31,0.38), sky), smoothstep(-0.4,0.2,R.y));
    float s1 = smoothstep(0.62, 0.94, R.y) * smoothstep(0.22, 0.85, 1.0 - abs(R.z));
    float s2 = smoothstep(0.32, 0.82, abs(R.x)) * smoothstep(-0.05, 0.35, R.y) * smoothstep(0.55, 0.0, R.y);
    float g = mix(1.0, 0.16, rough);
    return env + vec3(1.22,1.25,1.30)*s1*g + rim*0.75*s2*g;
  }
  vec3 aces(vec3 x){ return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0); }
  vec3 thermal(float t){
    float x=clamp(t,0.0,1.0);
    if(x<0.34) return mix(vec3(0.06,0.10,0.48), vec3(0.86,0.14,0.62), x/0.34);
    if(x<0.70) return mix(vec3(0.86,0.14,0.62), vec3(1.0,0.56,0.06), (x-0.34)/0.36);
    return mix(vec3(1.0,0.56,0.06), vec3(1.0,0.98,0.72), (x-0.70)/0.30);
  }
  void main(){
    vec3 V = normalize(uCam - vWp);
    float pulse = 0.5 + 0.5 * sin(uT * 5.0);
    if(uMode == 1){
      gl_FragColor = vec4(mix(vec3(0.18,0.85,1.0), vec3(1.0), uSel*pulse*0.65), 0.90);
      return;
    }
    if(uMode == 2){
      float fr = pow(1.0 - max(dot(normalize(vN), V), 0.0), 2.1);
      vec3 xc = mix(vec3(0.06,0.52,0.94), vec3(0.20,1.0,0.72), fr);
      if(uSel > 0.5) xc = mix(xc, vec3(1.0,0.85,0.25), 0.5 + 0.35*pulse);
      gl_FragColor = vec4(xc, clamp(0.16 + fr*0.74 + uSel*0.18, 0.15, 0.92) * uAlpha);
      return;
    }
    if(uMode == 4){
      float fr = pow(1.0 - max(dot(normalize(vN), V), 0.0), 2.0);
      float df = max(dot(normalize(vN), normalize(uKeyDir)), 0.0);
      vec3 tc = thermal(uHeat + fr*0.14 + df*0.10);
      if(uSel > 0.5) tc += vec3(0.24*pulse);
      gl_FragColor = vec4(tc, 1.0);
      return;
    }
    if(uMode == 5){
      float nv_bp = max(dot(normalize(vN), V), 0.0);
      float fr = pow(1.0 - nv_bp, 1.9);
      float df = max(dot(normalize(vN), normalize(uKeyDir)), 0.0);
      vec3 gx = abs(fract(vWp * 8.0) - 0.5);
      float gln = 1.0 - smoothstep(0.45, 0.495, max(gx.x, max(gx.y, gx.z)));
      vec3 bpCol = mix(vec3(0.04,0.16,0.40), vec3(0.10,0.36,0.70), 0.30 + 0.70*df);
      bpCol += vec3(0.32,0.90,1.0)*fr*1.15 + vec3(0.08,0.32,0.52)*gln*0.35;
      if(uSel > 0.5) bpCol = mix(bpCol, vec3(0.98,0.92,0.35), 0.45 + 0.35*pulse);
      gl_FragColor = vec4(bpCol, uAlpha);
      return;
    }
    vec3 tn = texture2D(uNrmTex, vUv).rgb * 2.0 - 1.0;
    vec3 N = normalize(vN + (vT * tn.x + vB * tn.y) * 0.55);
    vec3 texAlb = pow(max(texture2D(uAlbTex, vUv).rgb, vec3(0.0)), vec3(2.2));
    vec3 alb = texAlb * uBaseCol.rgb;
    vec3 mr = texture2D(uMrTex, vUv).rgb;
    float met = clamp(uMetallic * mr.b, 0.0, 1.0);
    float rgh = clamp(uRoughness * mr.g, 0.05, 0.96);
    float cc = uClearcoat;
    if(uMode == 3){
      float wv = 0.80 + 0.20 * step(0.0, sin(vWp.x*58.0)*sin((vWp.y+vWp.z)*58.0));
      alb *= mix(0.76, 0.98, wv);
      rgh = clamp(rgh * 0.52, 0.05, 0.62);
      cc = max(cc, 0.90);
    }
    float nv = max(dot(N, V), 0.001);
    vec3 F0 = mix(vec3(0.04), alb, met);
    vec3 Lo = vec3(0.0);
    vec3 L1 = normalize(uKeyDir);
    vec3 L2 = normalize(vec3(-uKeyDir.x*0.9, 0.35, -uKeyDir.z*0.6));
    vec3 L3 = normalize(vec3(0.15, 0.45, -1.0));
    for(int i=0;i<3;i++){
      vec3 L = (i==0)?L1:((i==1)?L2:L3);
      vec3 rad = (i==0)?uKeyCol:((i==1)?uFillCol:uRimCol);
      vec3 H = normalize(V + L);
      float nl = max(dot(N,L), 0.0), nh = max(dot(N,H), 0.0), hv = max(dot(H,V), 0.0);
      vec3 F = F0 + (1.0 - F0) * pow(1.0 - hv, 5.0);
      float D = D_GGX(nh, rgh), G = G_GGX(nv, rgh) * G_GGX(max(nl,0.001), rgh);
      vec3 sp = (D*G*F)/max(4.0*nv*max(nl,0.001), 0.001);
      vec3 kd = (1.0 - F) * (1.0 - met);
      float cSpec = cc * D_GGX(nh, 0.07) * 0.25 * pow(1.0 - hv, 3.0);
      Lo += (kd*alb/PI + sp + vec3(cSpec)) * rad * nl;
    }
    vec3 R = reflect(-V, N);
    vec3 kS = F0 + (max(vec3(1.0-rgh), F0) - F0) * pow(1.0 - nv, 5.0);
    vec3 env = studioEnv(R, rgh, uRimCol) * (F0 + kS*0.45) + studioEnv(R, 0.06, uRimCol) * cc * (0.04 + 0.72*pow(1.0-nv,4.0));
    vec3 irr = mix(vec3(0.07,0.08,0.11), vec3(0.24,0.27,0.32), 0.5 + 0.5*N.y);
    vec3 col = (1.0 - kS)*(1.0 - met)*alb*irr + env + Lo + uEmissive;
    if(uSel > 0.5) col += vec3(0.22,0.62,1.0) * (0.14 + 0.55*pow(1.0-nv,1.8)) * (0.55 + 0.45*pulse);
    col = pow(aces(col * 1.12), vec3(1.0/2.2));
    gl_FragColor = vec4(col, uAlpha);
  }`;

  function mkSh(t,s){const o=gl.createShader(t);gl.shaderSource(o,s);gl.compileShader(o);return o;}
  const pr = gl.createProgram();
  gl.attachShader(pr, mkSh(gl.VERTEX_SHADER, vsSrc));
  gl.attachShader(pr, mkSh(gl.FRAGMENT_SHADER, fsSrc));
  gl.linkProgram(pr);

  const aPos = gl.getAttribLocation(pr,'aPos'), aNorm = gl.getAttribLocation(pr,'aNorm');
  const aTan = gl.getAttribLocation(pr,'aTan'), aUv = gl.getAttribLocation(pr,'aUv');
  const uMvp = gl.getUniformLocation(pr,'uMvp'), uMod = gl.getUniformLocation(pr,'uModel');
  const uCam = gl.getUniformLocation(pr,'uCam'), uKeyDir = gl.getUniformLocation(pr,'uKeyDir');
  const uKeyCol = gl.getUniformLocation(pr,'uKeyCol'), uFillCol = gl.getUniformLocation(pr,'uFillCol'), uRimCol = gl.getUniformLocation(pr,'uRimCol');
  const uBaseCol = gl.getUniformLocation(pr,'uBaseCol'), uEmissive = gl.getUniformLocation(pr,'uEmissive');
  const uMet = gl.getUniformLocation(pr,'uMetallic'), uRgh = gl.getUniformLocation(pr,'uRoughness'), uCc = gl.getUniformLocation(pr,'uClearcoat');
  const uMode = gl.getUniformLocation(pr,'uMode'), uHeat = gl.getUniformLocation(pr,'uHeat'), uSel = gl.getUniformLocation(pr,'uSel');
  const uAlpha = gl.getUniformLocation(pr,'uAlpha'), uT = gl.getUniformLocation(pr,'uT');
  const uAlbTex = gl.getUniformLocation(pr,'uAlbTex'), uNrmTex = gl.getUniformLocation(pr,'uNrmTex'), uMrTex = gl.getUniformLocation(pr,'uMrTex');

  function makeFallbackTex(r,g,b){
    const tx=gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D,tx);
    gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,1,1,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([r,g,b,255]));
    return tx;
  }
  const fallbackWhite = makeFallbackTex(255,255,255), fallbackNorm = makeFallbackTex(128,128,255);

  let sceneParts = [], sceneTextures = [], currentLoadId = 0;

  function clearScene(){
    for(const p of sceneParts){ gl.deleteBuffer(p.vb); gl.deleteBuffer(p.nb); gl.deleteBuffer(p.tb); gl.deleteBuffer(p.ub); gl.deleteBuffer(p.ib); }
    for(const t of sceneTextures){ gl.deleteTexture(t); }
    sceneParts = []; sceneTextures = [];
  }

  function loadImgTex(bytes){
    const tx = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tx);
    gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,1,1,0,gl.RGBA,gl.UNSIGNED_BYTE,new Uint8Array([200,200,200,255]));
    const blob = new Blob([bytes], {type:'image/png'});
    const url = URL.createObjectURL(blob);
    const img = new Image();
    img.onload = ()=>{
      gl.bindTexture(gl.TEXTURE_2D, tx);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
      gl.generateMipmap(gl.TEXTURE_2D);
      URL.revokeObjectURL(url);
    };
    img.src = url;
    sceneTextures.push(tx);
    return tx;
  }

  window.loadPevGlb = async (url, meta) => {
    const lid = ++currentLoadId;
    try {
      const resp = await fetch(url);
      const ab = await resp.arrayBuffer();
      if(lid !== currentLoadId) return;
      const dv = new DataView(ab);
      if(dv.getUint32(0, true) !== 0x46546C67) return;
      const jsonLen = dv.getUint32(12, true);
      const jsonStr = new TextDecoder().decode(new Uint8Array(ab, 20, jsonLen));
      const g = JSON.parse(jsonStr);
      const binOff = 20 + jsonLen + 8;
      const binU8 = new Uint8Array(ab, binOff);
      const binBuf = ab.slice(binOff);

      clearScene();

      const imgs = (g.images||[]).map(im => {
        const bv = g.bufferViews[im.bufferView];
        return loadImgTex(binU8.subarray(bv.byteOffset||0, (bv.byteOffset||0) + bv.byteLength));
      });
      const texOf = (info) => info ? imgs[g.textures[info.index].source] : null;
      const getAcc = (idx) => {
        const acc = g.accessors[idx], bv = g.bufferViews[acc.bufferView];
        const off = bv.byteOffset || 0;
        if(acc.componentType === 5126) return new Float32Array(binBuf, off, acc.count * (acc.type==='VEC4'?4:acc.type==='VEC3'?3:acc.type==='VEC2'?2:1));
        return new Uint32Array(binBuf, off, acc.count);
      };
      const extUint = gl.getExtension('OES_element_index_uint');

      for(const node of (g.nodes||[])){
        if(node.mesh === undefined) continue;
        const rawName = node.name || '';
        const sep = rawName.indexOf('__part_');
        const nodeName = sep >= 0 ? rawName.slice(0, sep) : rawName;
        const partKey = sep >= 0 ? rawName.slice(sep + 7) : 'frame';
        const mesh = g.meshes[node.mesh];
        for(const prim of mesh.primitives){
          const pos = getAcc(prim.attributes.POSITION);
          const nrm = getAcc(prim.attributes.NORMAL);
          const tan = getAcc(prim.attributes.TANGENT);
          const uv = getAcc(prim.attributes.TEXCOORD_0);
          const idxRaw = getAcc(prim.indices);

          const vb=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,vb); gl.bufferData(gl.ARRAY_BUFFER,pos,gl.STATIC_DRAW);
          const nb=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,nb); gl.bufferData(gl.ARRAY_BUFFER,nrm,gl.STATIC_DRAW);
          const tb=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,tb); gl.bufferData(gl.ARRAY_BUFFER,tan,gl.STATIC_DRAW);
          const ub=gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER,ub); gl.bufferData(gl.ARRAY_BUFFER,uv,gl.STATIC_DRAW);

          let idxData = idxRaw, idxType = gl.UNSIGNED_INT;
          if(!extUint && !(gl instanceof WebGL2RenderingContext)){
            idxData = new Uint16Array(idxRaw.length);
            for(let k=0;k<idxRaw.length;k++) idxData[k] = idxRaw[k];
            idxType = gl.UNSIGNED_SHORT;
          }
          const ib=gl.createBuffer(); gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,ib); gl.bufferData(gl.ELEMENT_ARRAY_BUFFER,idxData,gl.STATIC_DRAW);

          const mat = g.materials[prim.material];
          const pbr = mat.pbrMetallicRoughness || {};
          const ccExt = mat.extensions && mat.extensions.KHR_materials_clearcoat;
          const bcf = pbr.baseColorFactor || [1,1,1,1];
          const ef = mat.emissiveFactor || [0,0,0];
          const axle = (meta && meta.axles && meta.axles[nodeName]) || null;
          const exp = (meta && meta.explodes && (meta.explodes[partKey] || meta.explodes[nodeName])) || [0,0,0];
          const isShell = partKey === 'frame' || partKey === 'cockpit';

          sceneParts.push({
            nodeName, partKey, isShell, axle, exp,
            vb, nb, tb, ub, ib, count: idxRaw.length, idxType,
            albTex: texOf(pbr.baseColorTexture) || fallbackWhite,
            nrmTex: texOf(mat.normalTexture) || fallbackNorm,
            mrTex: texOf(pbr.metallicRoughnessTexture) || fallbackWhite,
            baseCol: bcf, emissive: ef,
            metallic: pbr.metallicFactor ?? 0.5,
            roughness: pbr.roughnessFactor ?? 0.5,
            clearcoat: ccExt ? (ccExt.clearcoatFactor || 0) : 0
          });
        }
      }
    } catch(e) {}
  };

  function matPersp(fovy,asp,n,f){const g=1/Math.tan(fovy/2),nf=1/(n-f);return[g/asp,0,0,0,0,g,0,0,0,0,(f+n)*nf,-1,0,0,2*f*n*nf,0];}
  function matLook(ex,ey,ez){let zx=ex,zy=ey+0.05,zz=ez;const zl=Math.hypot(zx,zy,zz)||1;zx/=zl;zy/=zl;zz/=zl;
    let xx=zz,xy=0,xz=-zx;const xl=Math.hypot(xx,xy,xz)||1;xx/=xl;xz/=xl;
    const yx=zy*xz-zz*xy,yy=zz*xx-zx*xz,yz=zx*xy-zy*xx;
    return[xx,yx,zx,0, xy,yy,zy,0, xz,yz,zz,0, -(xx*ex+xy*ey+xz*ez),-(yx*ex+yy*ey+yz*ez),-(zx*ex+zy*ey+zz*ez),1];}
  function mul(a,b){const o=new Float32Array(16);for(let c=0;c<4;c++)for(let r=0;r<4;r++)o[c*4+r]=a[r]*b[c*4]+a[4+r]*b[c*4+1]+a[8+r]*b[c*4+2]+a[12+r]*b[c*4+3];return o;}
  function tr(x,y,z){return[1,0,0,0, 0,1,0,0, 0,0,1,0, x,y,z,1];}
  function rotZ(rad){const c=Math.cos(rad),s=Math.sin(rad);return[c,s,0,0, -s,c,0,0, 0,0,1,0, 0,0,0,1];}

  function buildMod(p){
    let m = [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1];
    if(p.partKey !== 'floor' && S.explode > 0.002){
      m = mul(tr(p.exp[0]*S.explode, p.exp[1]*S.explode, p.exp[2]*S.explode), m);
    }
    if(p.axle){
      const [ax,ay,az] = p.axle;
      m = mul(m, tr(ax,ay,az));
      m = mul(m, rotZ(-S.spin * Math.PI / 180));
      m = mul(m, tr(-ax,-ay,-az));
    }
    return m;
  }

  function frame(ts){
    const dpr=window.devicePixelRatio||1, w=canvas.clientWidth*dpr, h=canvas.clientHeight*dpr;
    if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h;}
    gl.viewport(0,0,w,h);
    if(S.mode==='BLUEPRINT') gl.clearColor(0.028,0.105,0.245,1);
    else if(S.light==='SUNSET') gl.clearColor(0.115,0.068,0.068,1);
    else if(S.light==='NEON') gl.clearColor(0.032,0.042,0.092,1);
    else gl.clearColor(0.055,0.072,0.102,1);
    gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
    gl.enable(gl.DEPTH_TEST); gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA);
    if(!sceneParts.length){ requestAnimationFrame(frame); return; }

    gl.useProgram(pr);
    const dist=4.65/Math.max(0.6,Math.min(2.4,S.zoom));
    const yr=S.yaw*Math.PI/180, prA=S.pitch*Math.PI/180;
    const cx=dist*Math.cos(prA)*Math.sin(yr), cy=dist*Math.sin(prA)+0.08, cz=dist*Math.cos(prA)*Math.cos(yr);
    const vp=mul(matPersp(38*Math.PI/180, w/Math.max(1,h), 0.25, 40), matLook(cx,cy,cz));
    const modeId = S.mode==='WIREFRAME'?1:S.mode==='XRAY'?2:S.mode==='CARBON_SHOWROOM'?3:S.mode==='THERMAL_IR'?4:S.mode==='BLUEPRINT'?5:0;

    if(S.light==='SUNSET'){
      gl.uniform3f(uKeyDir,-0.78,0.42,0.45); gl.uniform3f(uKeyCol,3.1,1.85,1.05); gl.uniform3f(uFillCol,0.65,0.60,1.05); gl.uniform3f(uRimCol,1.25,0.58,0.38);
    } else if(S.light==='NEON'){
      gl.uniform3f(uKeyDir,0.28,0.68,-0.68); gl.uniform3f(uKeyCol,1.35,2.45,3.0); gl.uniform3f(uFillCol,2.1,0.55,1.65); gl.uniform3f(uRimCol,0.35,1.65,2.2);
    } else {
      gl.uniform3f(uKeyDir,0.45,0.85,0.35); gl.uniform3f(uKeyCol,2.7,2.68,2.65); gl.uniform3f(uFillCol,0.95,1.05,1.25); gl.uniform3f(uRimCol,0.55,0.78,1.15);
    }
    gl.uniform3f(uCam,cx,cy,cz); gl.uniform1i(uMode,modeId); gl.uniform1f(uT,ts*0.001);
    gl.uniform1i(uAlbTex,0); gl.uniform1i(uNrmTex,1); gl.uniform1i(uMrTex,2);

    const tSec = ts*0.001, flashOn = Math.sin(tSec*8.5) > 0;
    const lSig = (S.sig==='LEFT'||S.sig==='HAZARD') && flashOn;
    const rSig = (S.sig==='RIGHT'||S.sig==='HAZARD') && flashOn;
    const ambB = Math.max(0, Math.min(0.25, (S.amb - 15)/40));
    const thrH = Math.max(0, Math.min(1, S.thr)) * 0.68;

    const drawPart = (p, alpha) => {
      const m = buildMod(p);
      gl.uniformMatrix4fv(uMod,false,m); gl.uniformMatrix4fv(uMvp,false,mul(vp,m));
      gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, p.albTex);
      gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, p.nrmTex);
      gl.activeTexture(gl.TEXTURE2); gl.bindTexture(gl.TEXTURE_2D, p.mrTex);
      let em = p.emissive;
      if(p.nodeName.includes('signal_left') && lSig) em = [2.6, 1.45, 0.08];
      else if(p.nodeName.includes('signal_right') && rSig) em = [2.6, 1.45, 0.08];
      const heatF = p.partKey==='motor'?0.92:p.partKey==='controller'?0.88:p.partKey==='battery'?0.72:p.partKey==='brakes'?0.65:0.22;
      gl.uniform4fv(uBaseCol, p.baseCol); gl.uniform3fv(uEmissive, em);
      gl.uniform1f(uMet, p.metallic); gl.uniform1f(uRgh, p.roughness); gl.uniform1f(uCc, p.clearcoat);
      gl.uniform1f(uHeat, Math.min(1, 0.12 + heatF*thrH + ambB));
      gl.uniform1f(uSel, (p.partKey!=='floor' && p.partKey===S.sel)?1.0:0.0);
      gl.uniform1f(uAlpha, alpha);

      gl.bindBuffer(gl.ARRAY_BUFFER,p.vb); gl.enableVertexAttribArray(aPos); gl.vertexAttribPointer(aPos,3,gl.FLOAT,false,0,0);
      gl.bindBuffer(gl.ARRAY_BUFFER,p.nb); gl.enableVertexAttribArray(aNorm); gl.vertexAttribPointer(aNorm,3,gl.FLOAT,false,0,0);
      gl.bindBuffer(gl.ARRAY_BUFFER,p.tb); gl.enableVertexAttribArray(aTan); gl.vertexAttribPointer(aTan,4,gl.FLOAT,false,0,0);
      gl.bindBuffer(gl.ARRAY_BUFFER,p.ub); gl.enableVertexAttribArray(aUv); gl.vertexAttribPointer(aUv,2,gl.FLOAT,false,0,0);
      gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER,p.ib);
      gl.drawElements(modeId===1 ? gl.LINES : gl.TRIANGLES, p.count, p.idxType, 0);
    };

    const isXray = modeId === 2;
    gl.depthMask(true);
    for(const p of sceneParts){ if(!isXray || !p.isShell) drawPart(p, 1.0); }
    if(isXray){
      gl.depthMask(false);
      for(const p of sceneParts){ if(p.isShell) drawPart(p, 0.72); }
      gl.depthMask(true);
    }
    requestAnimationFrame(frame);
  }
  requestAnimationFrame(frame);
}
</script></body></html>"""
