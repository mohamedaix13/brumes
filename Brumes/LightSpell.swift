import UIKit
import SceneKit

// MARK: - Light Spell (Lumière - Soin)
struct LightSpell: Spell {
    let name = "LUMIÈRE"
    let cost: Float = 15
    let cooldown: Float = 3.0
    let color: UIColor = .systemYellow
    
    func cast(from origin: SCNVector3, in scene: SCNScene, target: Enemy?, player: SCNNode, enemies: [Enemy], shrines: [SCNNode], active: Set<Int>, completion: @escaping (Float) -> Void) {
        // Soin : restaure la vie du joueur (à implémenter dans GameController)
        burst(at: origin, color: color, radius: 2, in: scene)
        completion(0.4)
    }
    
    private func distance(_ a: SCNVector3, _ b: SCNVector3) -> Float {
        return hypot(a.x - b.x, a.z - b.z)
    }
}