import CoreGraphics
let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as! [[String: Any]]
for w in list {
    let owner = w[kCGWindowOwnerName as String] as? String ?? "?"
    let pid = w[kCGWindowOwnerPID as String] ?? -1
    let num = w[kCGWindowNumber as String] ?? -1
    // bounds = 窗口屏幕坐标 (x y w h)，供 CGEvent 鼠标注入换算落点
    let b = w[kCGWindowBounds as String] as? [String: Any] ?? [:]
    let x = b["X"] ?? -1, y = b["Y"] ?? -1, wd = b["Width"] ?? -1, ht = b["Height"] ?? -1
    print("\(pid) \(num) \(owner) x=\(x) y=\(y) w=\(wd) h=\(ht)")
}
