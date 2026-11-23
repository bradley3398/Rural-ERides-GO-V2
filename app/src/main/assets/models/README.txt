Drop real .glb 3D PEV models into this directory (app/src/main/assets/models/):
- emoto.glb         (e-Moto & Electric Dirt Bikes)
- hyperscooter.glb  (Hyper-Scooters & Commuter Scooters)
- ebike_fat.glb     (Fat-Tire, Folding & Scrambler e-Bikes)
- euc.glb           (Electric Unicycles)
- hydrofoil.glb     (Electric Hydrofoils / Specialty)

The Vulkan Filament 1.81.0 PBR engine loads these assets via gltfio AssetLoader/ResourceLoader
and dynamically applies per-model PBR MaterialInstance overrides for all 58 PEV profiles.
