# Offline AI device profile: Redmi Note 13 4G (8 GB / 256 GB)

## Target device

- Device: Xiaomi Redmi Note 13 4G
- RAM/storage: 8 GB LPDDR4X / 256 GB UFS 2.2
- SoC: Qualcomm Snapdragon 685 (4x Cortex-A73 up to 2.8 GHz + 4x Cortex-A53 up to 1.9 GHz)
- GPU: Adreno 610
- Android ABI target: arm64-v8a (confirm at runtime before native library/model load)

Official device references:
- https://www.mi.com/pk/product/redmi-note-13/specs/
- https://www.mi.com/pk/support/faq/details/KA-91513/

## Initial model decision

Default candidate: Qwen3-1.7B GGUF, Q4_K_M (about 1.28 GB).

This is a conservative first model for an 8 GB phone. It is a candidate, not a device-validated performance claim. The exact GGUF artifact must be pinned by repository, revision, filename, license, and SHA-256 before shipping. Prefer the official ggml-org GGUF conversion when its metadata and model template are verified.

- Model listing: https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF
- Upstream Android runtime guide: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md

Do not make a 4B model the default. It may be evaluated later as an optional download only if real-device memory, latency, heat, and stability benchmarks pass.

## Runtime design

1. Integrate llama.cpp through a maintained Android native/JNI module or its Android binding. Build only arm64-v8a for this device profile initially; retain a safe fallback for other ABIs.
2. Store the model in app-private files, not in the APK or Git repository. Download only after the owner explicitly chooses Offline AI; show size, checksum, license, storage requirement, and progress.
3. Verify SHA-256 before loading. Support pause/resume, low-storage handling, model deletion, and versioned model metadata.
4. Start with a 4096-token context. Make context configurable only after measuring memory; do not assume longer context is free.
5. Use CPU inference as the baseline. Detect compatible acceleration at runtime; do not force unsupported GPU/Vulkan paths on Adreno 610.
6. Run inference off the UI thread, stream tokens to Compose, support cancellation, and serialize model access to avoid concurrent memory spikes.
7. Keep the model unloaded until first offline use; release it after an idle timeout or on memory pressure. Do not keep a second model resident.
8. Keep online routing as primary for difficult tasks when connectivity exists. Offline model is the privacy-preserving/basic fallback, not a substitute for current web research or large-model reasoning.
9. Keep local memory encrypted and retrieve only relevant short context; never append the entire conversation history to every prompt.
10. Show clear status: model missing/downloading/ready/loading/generating/unloaded/error; expose an explicit unload and delete action.

## Initial resource guardrails (to be benchmarked, not promises)

- Model artifact target: about 1.3 GB.
- Initial context: 4096 tokens.
- Start with one inference session and conservative thread count; benchmark 2, 4, and 6 threads rather than assuming more is faster.
- Keep a substantial Android memory reserve; abort or unload on low-memory callbacks.
- Do not use Android "RAM expansion" as if it were physical RAM.
- Avoid loading model weights into a long-lived duplicate byte array; use memory mapping where supported by the selected runtime.

## Device validation gates

Run on the owner's actual Redmi Note 13 4G, plugged/unplugged and with normal background apps:

- Cold model load and warm reload time
- First-token latency and generated tokens/second on short and 1k-token prompts
- Peak PSS/RSS and Android low-memory behavior
- 10-minute continuous generation, cancellation, screen rotation/background/foreground
- Device temperature and battery drain
- Airplane-mode conversation and local-memory retrieval
- App restart, model checksum failure, interrupted download, insufficient storage
- No UI jank or crash; model unload restores memory

Do not label offline AI "implemented" until these tests pass on physical hardware and a release artifact is verified.

## Next engineering steps

1. Add model/runtime abstraction and capability detection.
2. Add model manager and checksum-verified download flow.
3. Integrate llama.cpp Android arm64-v8a runtime.
4. Add offline routing and bounded local-memory context.
5. Add instrumentation/benchmark screens and airplane-mode tests.
6. Produce a signed APK and validate on the target phone.

## Scope and safety

Offline model inference does not grant Android phone-control permissions. Phone actions remain a separate permission-gated action system with explicit confirmation for consequential actions and a universal stop control.
