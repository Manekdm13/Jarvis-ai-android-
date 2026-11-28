# JARVIS 21-section master source map and Ollama setup

This branch contains the 21-section capability registry at:
`app/src/main/java/com/manekdm13/jarvis/JarvisMasterCapabilities.kt`

It is a truthful capability inventory, not a claim that all listed features are already implemented. States are explicit: PLANNED, FOUNDATION, AVAILABLE, BLOCKED.

## Local Ollama inference

The Android app now has `JarvisOllamaClient.kt`, exposed through:
- `AiGatewayClient.localModels(ollamaBaseUrl)`
- `AiGatewayClient.localReply(ollamaBaseUrl, model, systemPrompt, message, history)`

### Network setup
1. Install Ollama on a computer/server on the same trusted Wi-Fi network as the phone.
2. Pull a model suitable for the server's RAM/VRAM, for example a small quantized model.
3. Configure Ollama to listen on a private LAN interface, not an internet-exposed public interface.
4. Permit the phone to reach that host through the local network firewall.
5. In the app integration, use the server's private address, such as `http://192.168.x.x:11434`.
6. Call `localModels` to list installed models, then `localReply` to run inference.

Android localhost is the phone itself; it is not the computer running Ollama. The adapter accepts HTTPS generally and HTTP only for common private LAN host ranges or .local/localhost.

### Device reality
Ollama is usually run on a separate computer/server. The Redmi Note 13 4G does not automatically host an Ollama server merely because this client is installed. A phone-local model runtime needs a separate Android-compatible runtime and device-performance testing.

## 21 sections
1. Independent JARVIS brain
2. Continuous self-learning
3. Skill acquisition and improvement
4. Autonomous task execution
5. Android phone control
6. Voice and wake word
7. Online and offline intelligence
8. Persistent memory
9. Cloud infrastructure
10. Research and general knowledge
11. Browser and computer use
12. Personal productivity
13. Scholarships and education
14. Career and job assistance
15. Legitimate online earning
16. Security and privacy
17. Futuristic user interface
18. Updates and maintenance
19. Diagnostics and self-repair
20. Extensibility and integrations
21. Continuous intelligence engine

Each feature must be marked AVAILABLE only after implementation, tests and acceptance checks. Sensitive external actions remain approval-gated.
