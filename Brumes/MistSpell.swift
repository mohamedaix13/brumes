import UIKit
import SceneKit

// MARK: - Mist Spell (Brume)
struct MistSpell: Spell {
    let name = "BRUME"
    let cost: Float = 20
    let cooldown: Float = 4.0
    let color: UIColor = .systemGray
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        // Crée un nuage de brume qui ralentit les ennemis
        burst(at: origin, color: color, radius: 6, in: scene)
        for e in enemies where e.health > 0 && distance(origin, e.node.position) < 6 {
            // Ralentit les ennemis (freeze = ralentissement)
            hit(e, damage: 15, freeze: 5.0)
        }
        completion(0.8)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}