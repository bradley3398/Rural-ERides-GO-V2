Optional pre-baked Filament cmgen studio HDRI environments (app/src/main/assets/envs/):
- studio_ibl.ktx / studio_skybox.ktx
- sunset_ibl.ktx / sunset_skybox.ktx
- neon_ibl.ktx   / neon_skybox.ktx

When .ktx files are not present, the engine automatically synthesizes a procedural
studio spherical-harmonics + mipmapped softbox cubemap IBL and cyclorama skybox.
