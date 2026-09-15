import UIKit
import SceneKit

// MARK: - Wave Spell (Onde)
struct WaveSpell: Spell {
    let name = "ONDE"
    let cost: Float = 38
    let cooldown: Float = 5.0
    let color: UIColor = .systemPurple
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        burst(at: origin, color: color, radius: 8, in: scene)
        for e in enemies where e.health > 0 && distance(origin, e.node.position) < 8 {
            hit(e, damage: 65, freeze: 0.8)
        }
        completion(0.5)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}