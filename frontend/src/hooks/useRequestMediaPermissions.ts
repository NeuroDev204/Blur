import { useEffect } from "react"

// Xin quyen camera + micro ngay khi vao web (ca PC lan dien thoai), thay vi doi den luc goi moi xin.
// Cach lam: goi getUserMedia de hien prompt cua trinh duyet, roi stop tat ca track ngay lap tuc
// de KHONG giu camera/micro (den camera khong sang). Sau khi user da cho phep, luc goi se khong hoi lai.

const decided = (state: PermissionState | "unsupported") =>
  state === "granted" || state === "denied"

async function getPermissionState(
  name: "camera" | "microphone"
): Promise<PermissionState | "unsupported"> {
  try {
    if (!navigator.permissions?.query) return "unsupported"
    // 'camera'/'microphone' chua co trong type PermissionName cua TS nen phai cast.
    const status = await navigator.permissions.query({ name: name as PermissionName })
    return status.state
  } catch {
    // Mot so trinh duyet (vd Firefox) khong query duoc camera/mic -> coi nhu chua biet.
    return "unsupported"
  }
}

async function requestOnce(): Promise<"done" | "retry"> {
  if (!navigator.mediaDevices?.getUserMedia) return "done"
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true })
    // Tha thiet bi ngay - chi can prompt, khong giu camera/micro.
    stream.getTracks().forEach((track) => track.stop())
    return "done"
  } catch (error) {
    const name = (error as { name?: string })?.name
    // User tu choi han, hoac may khong co camera/micro -> khong retry nua.
    if (name === "NotAllowedError" || name === "PermissionDeniedError" || name === "NotFoundError") {
      return "done"
    }
    // SecurityError (can HTTPS), can user gesture (mobile), hoac thiet bi dang ban -> thu lai khi user tuong tac.
    return "retry"
  }
}

/**
 * Xin quyen camera + micro mot lan ngay khi mount (vao web). Goi o tang App de ap dung toan trang.
 */
export function useRequestMediaPermissions() {
  useEffect(() => {
    let cancelled = false
    let cleanupGesture: (() => void) | undefined

    const run = async () => {
      const [cam, mic] = await Promise.all([
        getPermissionState("camera"),
        getPermissionState("microphone"),
      ])
      // Da quyet dinh ca hai (granted/denied) thi khong can hoi lai.
      if (decided(cam) && decided(mic)) return

      if (cancelled) return
      const result = await requestOnce()
      if (cancelled || result === "done") return

      // Tren mobile, getUserMedia thuong bi chan neu chua co thao tac nguoi dung.
      // Thu lai o lan tuong tac dau tien (cham/click/go phim).
      const onGesture = () => {
        cleanupGesture?.()
        void requestOnce()
      }
      const opts: AddEventListenerOptions = { once: true, passive: true }
      window.addEventListener("pointerdown", onGesture, opts)
      window.addEventListener("touchstart", onGesture, opts)
      window.addEventListener("keydown", onGesture, opts)
      cleanupGesture = () => {
        window.removeEventListener("pointerdown", onGesture)
        window.removeEventListener("touchstart", onGesture)
        window.removeEventListener("keydown", onGesture)
      }
    }

    void run().catch((e) => console.error("Media permission priming failed:", e))

    return () => {
      cancelled = true
      cleanupGesture?.()
    }
  }, [])
}
