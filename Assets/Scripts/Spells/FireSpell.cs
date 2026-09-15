using UnityEngine;

public class FireSpell : MonoBehaviour
{
    public float speed = 20f;
    public float lifetime = 3f;
    public int damage = 25;
    public GameObject explosionPrefab;
    public ElementType elementType = ElementType.Fire;
    
    private float timer = 0f;
    
    void Start()
    {
        Destroy(gameObject, lifetime);
    }
    
    void Update()
    {
        transform.Translate(Vector3.forward * speed * Time.deltaTime);
        timer += Time.deltaTime;
        if (timer >= lifetime)
        {
            Explode();
        }
    }
    
    void OnTriggerEnter(Collider other)
    {
        if (other.CompareTag("Enemy"))
        {
            other.GetComponent<EnemyHealth>()?.TakeDamage(damage, elementType);
            Explode();
        }
        else if (other.CompareTag("Wall"))
        {
            Explode();
        }
    }
    
    void Explode()
    {
        if (explosionPrefab != null)
        {
            Instantiate(explosionPrefab, transform.position, Quaternion.identity);
        }
        Destroy(gameObject);
    }
}

public enum ElementType
{
    Fire, Ice, Lightning, Mist, Light, Shadow
}