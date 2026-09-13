import UIKit
import SceneKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?
    func application(_ application: UIApplication, didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = GameController()
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}

final class Stick: UIView {
    var value = CGPoint.zero
    private let knob = UIView()
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = UIColor.white.withAlphaComponent(0.08)
        layer.cornerRadius = 60
        layer.borderWidth = 1
        layer.borderColor = UIColor.white.withAlphaComponent(0.25).cgColor
        knob.backgroundColor = UIColor.white.withAlphaComponent(0.4)
        knob.frame = CGRect(x: 40, y: 40, width: 40, height: 40)
        knob.layer.cornerRadius = 20
        knob.isUserInteractionEnabled = false
        addSubview(knob)
    }
    required init?(coder: NSCoder) { fatalError() }
    private func move(_ touches: Set<UITouch>) {
        guard let p = touches.first?.location(in: self) else { return }
        let x = p.x - 60, y = p.y - 60
        let length = max(40, hypot(x, y))
        value = CGPoint(x: x / length, y: y / length)
        knob.center = CGPoint(x: 60 + value.x * 40, y: 60 + value.y * 40)
    }
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) { move(touches) }
    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) { move(touches) }
    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { reset() }
    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { reset() }
    func reset() { value = .zero; knob.center = CGPoint(x: 60, y: 60) }
}

final class Enemy {
    let node: SCNNode
    var health: Float = 100
    var frozen: Float = 0
    let home: SCNVector3
    init(_ node: SCNNode) { self.node = node; home = node.position }
}

final class GameController: UIViewController {
    private let scene = SCNScene()
    private let sceneView = SCNView()
    private let player = SCNNode()
    private let camera = SCNNode()
    private let stick = Stick(frame: .zero)
    private let hud = UILabel()
    private let notice = UILabel()
    private var buttons: [UIButton] = []
    private var enemies: [Enemy] = []
    private var shrines: [SCNNode] = []
    private var active = Set<Int>()
    private var obstacles: [(Float, Float, Float)] = []
    private var health: Float = 100
    private var mana: Float = 100
    private var cooldowns: [Float] = [0, 0, 0]
    private var facing: Float = 0
    private var clock: CADisplayLink?
    private var lastTime: CFTimeInterval = 0
    private var saveTime: Float = 0
    private var messageTime: Float = 12
    private var kills = 0
    private var seed: UInt64 = 926
    override var prefersStatusBarHidden: Bool { true }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .landscape }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        sceneView.frame = view.bounds
        sceneView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        sceneView.scene = scene
        sceneView.isPlaying = true
        sceneView.preferredFramesPerSecond = 30
        sceneView.antialiasingMode = .multisampling2X
        view.addSubview(sceneView)
        buildWorld()
        buildHUD()
        loadGame()
        updateCamera()
        NotificationCenter.default.addObserver(self, selector: #selector(suspend), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(resume), name: UIApplication.didBecomeActiveNotification, object: nil)
        clock = CADisplayLink(target: self, selector: #selector(tick(_:)))
        clock?.preferredFramesPerSecond = 30
        clock?.add(to: .main, forMode: .common)
    }

    private func random(_ low: Float, _ high: Float) -> Float {
        seed = seed &* 6364136223846793005 &+ 1
        return low + Float((seed >> 32) % 10000) / 10000 * (high - low)
    }
    private func material(_ color: UIColor, glow: Bool = false) -> SCNMaterial {
        let m = SCNMaterial()
        m.diffuse.contents = color
        m.roughness.contents = 0.85
        if glow { m.emission.contents = color }
        return m
    }
    @discardableResult private func object(_ geometry: SCNGeometry, _ color: UIColor, _ position: SCNVector3, parent: SCNNode? = nil, glow: Bool = false) -> SCNNode {
        geometry.materials = [material(color, glow: glow)]
        let n = SCNNode(geometry: geometry)
        n.position = position
        (parent ?? scene.rootNode).addChildNode(n)
        return n
    }
    private func buildWorld() {
        let sky = UIColor(red: 0.075, green: 0.12, blue: 0.19, alpha: 1)
        scene.background.contents = sky
        scene.fogColor = sky
        scene.fogStartDistance = 40
        scene.fogEndDistance = 105
        let ambient = SCNNode()
        ambient.light = SCNLight()
        ambient.light?.type = .ambient
        ambient.light?.color = UIColor(red: 0.43, green: 0.53, blue: 0.68, alpha: 1)
        ambient.light?.intensity = 650
        scene.rootNode.addChildNode(ambient)
        let sun = SCNNode()
        sun.light = SCNLight()
        sun.light?.type = .directional
        sun.light?.intensity = 1100
        sun.light?.color = UIColor(red: 0.7, green: 0.79, blue: 1, alpha: 1)
        sun.eulerAngles = SCNVector3(-0.8, -0.5, 0)
        scene.rootNode.addChildNode(sun)
        object(SCNBox(width: 180, height: 1, length: 180, chamferRadius: 0), UIColor(red: 0.12, green: 0.2, blue: 0.18, alpha: 1), SCNVector3(0, -0.5, 0))
        // All gameplay takes place on a flat valley floor; distant peaks are decorative.
        for i in 0..<36 {
            let a = Float(i) / 36 * Float.pi * 2
            let h = random(15, 32)
            object(SCNCone(topRadius: 1, bottomRadius: CGFloat(random(10, 20)), height: CGFloat(h)), UIColor(red: 0.13, green: 0.19, blue: 0.23, alpha: 1), SCNVector3(sin(a) * 98, h / 2 - 1, cos(a) * 98))
        }
        let locations: [SCNVector3] = [SCNVector3(-48, 0, -42), SCNVector3(52, 0, -35), SCNVector3(20, 0, 57)]
        for (i, location) in locations.enumerated() {
            let shrine = SCNNode()
            shrine.position = location
            scene.rootNode.addChildNode(shrine)
            object(SCNCylinder(radius: 3.5, height: 0.3), .darkGray, SCNVector3(0, 0.15, 0), parent: shrine)
            let crystal = object(SCNPyramid(width: 1.7, height: 3.5, length: 1.7), .cyan, SCNVector3(0, 2, 0), parent: shrine, glow: true)
            crystal.name = "crystal"
            crystal.runAction(.repeatForever(.rotateBy(x: 0, y: 2, z: 0, duration: 3)))
            for j in 0..<5 {
                let a = Float(j) / 5 * .pi * 2
                let x = sin(a) * 5, z = cos(a) * 5
                object(SCNBox(width: 0.8, height: 4, length: 0.8, chamferRadius: 0.12), .gray, SCNVector3(x, 2, z), parent: shrine)
                obstacles.append((location.x + x, location.z + z, 0.85))
            }
            shrine.name = "Sanctuaire \(i + 1)"
            shrines.append(shrine)
        }
        for _ in 0..<140 {
            let x = random(-84, 84), z = random(-84, 84)
            if hypot(x, z) < 10 || locations.contains(where: { hypot(x - $0.x, z - $0.z) < 10 }) { continue }
            let tree = SCNNode()
            tree.position = SCNVector3(x, 0, z)
            object(SCNCylinder(radius: 0.3, height: 2.8), .brown, SCNVector3(0, 1.4, 0), parent: tree)
            object(SCNCone(topRadius: 0, bottomRadius: 2, height: 5.5), UIColor(red: 0.08, green: 0.23, blue: 0.2, alpha: 1), SCNVector3(0, 4.4, 0), parent: tree)
            scene.rootNode.addChildNode(tree.flattenedClone())
            obstacles.append((x, z, 0.8))
        }
        for _ in 0..<24 {
            let x = random(-80, 80), z = random(-80, 80)
            if hypot(x, z) < 12 || locations.contains(where: { hypot(x - $0.x, z - $0.z) < 9 }) { continue }
            let rock = object(SCNSphere(radius: 1.5), UIColor(white: 0.26, alpha: 1), SCNVector3(x, 0.5, z))
            rock.scale = SCNVector3(1, 0.7, 0.8)
            obstacles.append((x, z, 1.9))
        }
        scene.rootNode.addChildNode(player)
        object(SCNCone(topRadius: 0.28, bottomRadius: 0.62, height: 1.5), .systemIndigo, SCNVector3(0, 0.85, 0), parent: player)
        object(SCNSphere(radius: 0.26), UIColor(red: 0.78, green: 0.62, blue: 0.48, alpha: 1), SCNVector3(0, 1.8, 0), parent: player)
        object(SCNCone(topRadius: 0, bottomRadius: 0.47, height: 0.8), .systemIndigo, SCNVector3(0, 2.3, 0), parent: player)
        object(SCNCylinder(radius: 0.055, height: 2.2), .brown, SCNVector3(0.65, 1.1, 0), parent: player)
        object(SCNSphere(radius: 0.17), .cyan, SCNVector3(0.65, 2.3, 0), parent: player, glow: true)
        for i in 0..<18 {
            let a = Float(i) * 2.399
            let r = Float(18 + (i % 5) * 12)
            let node = SCNNode()
            node.position = SCNVector3(sin(a) * r, 0, cos(a) * r)
            object(SCNCapsule(capRadius: 0.48, height: 1.8), UIColor(red: 0.25, green: 0.1, blue: 0.22, alpha: 1), SCNVector3(0, 1, 0), parent: node)
            object(SCNSphere(radius: 0.16), .systemPink, SCNVector3(0, 1.6, -0.42), parent: node, glow: true)
            scene.rootNode.addChildNode(node)
            enemies.append(Enemy(node))
        }
        camera.camera = SCNCamera()
        camera.camera?.zFar = 160
        camera.camera?.fieldOfView = 62
        camera.camera?.wantsHDR = true
        camera.camera?.bloomIntensity = 0.65
        camera.camera?.bloomThreshold = 0.7
        camera.camera?.bloomBlurRadius = 5
        scene.rootNode.addChildNode(camera)
        sceneView.pointOfView = camera
    }

    private func buildHUD() {
        hud.font = .monospacedSystemFont(ofSize: 13, weight: .semibold)
        hud.textColor = .white
        hud.numberOfLines = 3
        hud.backgroundColor = UIColor.black.withAlphaComponent(0.35)
        hud.layer.cornerRadius = 12
        hud.clipsToBounds = true
        view.addSubview(hud)
        notice.font = .systemFont(ofSize: 14, weight: .medium)
        notice.textColor = .white
        notice.textAlignment = .center
        notice.numberOfLines = 2
        notice.text = "BRUMES — Les trois sanctuaires\nExplore la vallée et approche les cristaux pour les éveiller."
        notice.backgroundColor = UIColor.black.withAlphaComponent(0.35)
        view.addSubview(notice)
        view.addSubview(stick)
        for (i, title) in ["FEU", "GIVRE", "ONDE"].enumerated() {
            let button = UIButton(type: .system)
            button.tag = i
            button.setTitle(title, for: .normal)
            button.titleLabel?.font = .boldSystemFont(ofSize: 13)
            button.setTitleColor(.white, for: .normal)
            button.backgroundColor = [UIColor.systemOrange, .systemCyan, .systemPurple][i].withAlphaComponent(0.65)
            button.layer.cornerRadius = 32
            button.addTarget(self, action: #selector(cast(_:)), for: .touchUpInside)
            view.addSubview(button)
            buttons.append(button)
        }
        let pan = UIPanGestureRecognizer(target: self, action: #selector(rotateCamera(_:)))
        sceneView.addGestureRecognizer(pan)
    }
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let s = view.safeAreaInsets, w = view.bounds.width, h = view.bounds.height
        hud.frame = CGRect(x: s.left + 12, y: s.top + 10, width: 310, height: 72)
        notice.frame = CGRect(x: w / 2 - 205, y: s.top + 90, width: 410, height: 50)
        stick.frame = CGRect(x: s.left + 24, y: h - s.bottom - 140, width: 120, height: 120)
        for (i, b) in buttons.enumerated() {
            b.frame = CGRect(x: w - s.right - 92 - CGFloat(i) * 78, y: h - s.bottom - 95, width: 68, height: 68)
        }
    }
    private var cameraAngle: Float = 0
    @objc private func rotateCamera(_ gesture: UIPanGestureRecognizer) {
        cameraAngle -= Float(gesture.translation(in: sceneView).x) * 0.006
        gesture.setTranslation(.zero, in: sceneView)
    }
    private func updateCamera() {
        let p = player.position
        camera.position = SCNVector3(p.x + sin(cameraAngle) * 10, 7, p.z + cos(cameraAngle) * 10)
        camera.look(at: SCNVector3(p.x, 1.2, p.z))
    }
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float { hypot(a.x - b.x, a.z - b.z) }
    private func canWalk(_ x: Float, _ z: Float) -> Bool {
        abs(x) < 85 && abs(z) < 85 && !obstacles.contains { hypot(x - $0.0, z - $0.1) < $0.2 }
    }
    @objc private func tick(_ link: CADisplayLink) {
        let dt = Float(min(max(link.timestamp - lastTime, 0), 0.05))
        lastTime = link.timestamp
        let vx = Float(stick.value.x), vz = Float(stick.value.y)
        let dx = (vx * cos(cameraAngle) + vz * sin(cameraAngle)) * dt * 7
        let dz = (-vx * sin(cameraAngle) + vz * cos(cameraAngle)) * dt * 7
        if abs(dx) + abs(dz) > 0.001 {
            facing = atan2(dx, dz)
            player.eulerAngles.y = facing
            if canWalk(player.position.x + dx, player.position.z) { player.position.x += dx }
            if canWalk(player.position.x, player.position.z + dz) { player.position.z += dz }
        }
        mana = min(100, mana + dt * 9)
        for i in 0..<3 { cooldowns[i] = max(0, cooldowns[i] - dt) }
        for e in enemies where e.health > 0 {
            let d = distance(e.node.position, player.position)
            e.frozen = max(0, e.frozen - dt)
            if d < 17 && d > 1.3 && e.frozen == 0 {
                let speed = dt * 2.6
                let x = e.node.position.x + (player.position.x - e.node.position.x) / d * speed
                let z = e.node.position.z + (player.position.z - e.node.position.z) / d * speed
                if canWalk(x, e.node.position.z) { e.node.position.x = x }
                if canWalk(e.node.position.x, z) { e.node.position.z = z }
                e.node.look(at: SCNVector3(player.position.x, 0, player.position.z))
            }
            if d < 1.6 && e.frozen == 0 { health -= dt * 11 }
        }
        for (i, shrine) in shrines.enumerated() where !active.contains(i) {
            if distance(player.position, shrine.position) < 3.8 {
                active.insert(i)
                shrine.childNode(withName: "crystal", recursively: false)?.geometry?.materials = [material(.systemYellow, glow: true)]
                health = 100; mana = 100
                burst(at: shrine.position, color: .systemYellow, radius: 4)
                show(active.count == 3 ? "La vallée est libérée !\nLes trois sanctuaires sont éveillés. Continue ton exploration." : "Sanctuaire éveillé : \(active.count)/3 — vie et mana restaurées.")
                saveGame()
            }
        }
        if health <= 0 {
            player.position = SCNVector3Zero
            health = 100; mana = 100
            for e in enemies where e.health > 0 { e.node.position = e.home }
            show("Les brumes te ramènent au refuge. Les sanctuaires restent éveillés.")
        }
        let target = shrines.enumerated().filter { !active.contains($0.offset) }.min { distance(player.position, $0.element.position) < distance(player.position, $1.element.position) }
        let goal = target.map { item -> String in
            let p = item.element.position
            let angle = atan2(p.x - player.position.x, -(p.z - player.position.z)) + cameraAngle
            let directions = ["↑", "↗", "→", "↘", "↓", "↙", "←", "↖"]
            let sector = (Int((angle / (.pi / 4)).rounded()) % 8 + 8) % 8
            return "\(directions[sector]) Sanctuaire \(item.offset + 1) : \(Int(distance(player.position, p))) m"
        } ?? "Vallée libérée"
        hud.text = "  BRUMES  ·  Vie \(Int(health))  ·  Mana \(Int(mana))\n  Sanctuaires \(active.count)/3  ·  Ombres vaincues \(kills)\n  \(goal)"
        let names = ["FEU", "GIVRE", "ONDE"], costs: [Float] = [14, 25, 38]
        for i in 0..<3 {
            buttons[i].setTitle(cooldowns[i] > 0 ? String(format: "%.1fs", cooldowns[i]) : names[i], for: .normal)
            buttons[i].alpha = cooldowns[i] > 0 || mana < costs[i] ? 0.4 : 1
        }
        messageTime -= dt
        notice.isHidden = messageTime <= 0
        saveTime += dt
        if saveTime > 5 { saveGame(); saveTime = 0 }
        updateCamera()
    }
    private func show(_ message: String) { notice.text = message; messageTime = 7; notice.isHidden = false }
    @objc private func cast(_ sender: UIButton) {
        let i = sender.tag, costs: [Float] = [14, 25, 38]
        guard cooldowns[i] <= 0, mana >= costs[i] else { return }
        mana -= costs[i]
        cooldowns[i] = [0.55, 3.5, 5.0][i]
        let origin = player.position
        if i == 2 {
            burst(at: origin, color: .systemPurple, radius: 8)
            for e in enemies where e.health > 0 && distance(origin, e.node.position) < 8 { hit(e, damage: 65, freeze: 0.8) }
        } else {
            let color: UIColor = i == 0 ? .systemOrange : .systemCyan
            let target = enemies.filter { $0.health > 0 && distance(origin, $0.node.position) < 24 }.min { distance(origin, $0.node.position) < distance(origin, $1.node.position) }
            let destination = target?.node.position ?? SCNVector3(origin.x + sin(facing) * 20, 0, origin.z + cos(facing) * 20)
            let projectile = object(SCNSphere(radius: 0.23), color, SCNVector3(origin.x, 1.4, origin.z), glow: true)
            let trail = particles(color, count: 55, life: 0.3)
            projectile.addParticleSystem(trail)
            let duration = TimeInterval(max(0.12, distance(origin, destination) / 28))
            projectile.runAction(.sequence([.move(to: SCNVector3(destination.x, 1.1, destination.z), duration: duration), .run { [weak self, weak projectile] _ in
                guard let self = self else { return }
                self.burst(at: destination, color: color, radius: i == 0 ? 1.5 : 3)
                if let e = target, e.health > 0, self.distance(e.node.position, destination) < 3 {
                    self.hit(e, damage: i == 0 ? 42 : 25, freeze: i == 0 ? 0 : 3)
                }
                projectile?.removeFromParentNode()
            }]))
        }
    }
    private func hit(_ e: Enemy, damage: Float, freeze: Float) {
        e.health -= damage
        e.frozen = max(e.frozen, freeze)
        if e.health <= 0 {
            kills += 1
            mana = min(100, mana + 12)
            e.node.runAction(.sequence([.fadeOut(duration: 0.3), .removeFromParentNode()]))
        }
    }
    private func particles(_ color: UIColor, count: CGFloat, life: CGFloat) -> SCNParticleSystem {
        let p = SCNParticleSystem()
        p.birthRate = count
        p.particleLifeSpan = life
        p.particleSize = 0.1
        p.particleColor = color
        p.particleVelocity = 1.5
        p.spreadingAngle = 180
        p.blendMode = .additive
        p.isLightingEnabled = false
        return p
    }
    private func burst(at position: SCNVector3, color: UIColor, radius: CGFloat) {
        let n = SCNNode()
        n.position = SCNVector3(position.x, 0.9, position.z)
        scene.rootNode.addChildNode(n)
        let p = particles(color, count: 230, life: 0.45)
        p.emissionDuration = 0.12
        p.loops = false
        p.particleVelocity = radius * 2
        n.addParticleSystem(p)
        let ring = object(SCNTorus(ringRadius: 0.35, pipeRadius: 0.035), color, SCNVector3(0, -0.65, 0), parent: n, glow: true)
        ring.runAction(.group([.scale(to: radius * 2, duration: 0.5), .fadeOut(duration: 0.5)]))
        n.runAction(.sequence([.wait(duration: 1), .removeFromParentNode()]))
    }
    @objc private func suspend() { saveGame(); stick.reset(); clock?.isPaused = true; sceneView.isPlaying = false }
    @objc private func resume() { lastTime = 0; clock?.isPaused = false; sceneView.isPlaying = true }
    private func saveGame() {
        let state: [String: Any] = ["x": player.position.x, "z": player.position.z, "active": Array(active), "kills": kills, "dead": enemies.enumerated().filter { $0.element.health <= 0 }.map { $0.offset }]
        UserDefaults.standard.set(state, forKey: "brumes.v1")
    }
    private func loadGame() {
        guard let s = UserDefaults.standard.dictionary(forKey: "brumes.v1") else { return }
        let x = (s["x"] as? NSNumber)?.floatValue ?? 0, z = (s["z"] as? NSNumber)?.floatValue ?? 0
        if canWalk(x, z) { player.position = SCNVector3(x, 0, z) }
        active = Set((s["active"] as? [Int] ?? []).filter { shrines.indices.contains($0) })
        kills = s["kills"] as? Int ?? 0
        for i in active { shrines[i].childNode(withName: "crystal", recursively: false)?.geometry?.materials = [material(.systemYellow, glow: true)] }
        for i in s["dead"] as? [Int] ?? [] where enemies.indices.contains(i) { enemies[i].health = 0; enemies[i].node.removeFromParentNode() }
    }
}
