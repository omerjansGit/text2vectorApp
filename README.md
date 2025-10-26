This project will be used mostly for ollama and text embeddings.


För att köra applikationen och testa:
1. installera ollama:
   https://ollama.com/download
3. dra ner modellen nomic-embed-text:latest:
   ollama pull nomic-embed-text
4. starta applikationen i intellij
5. gör en curl anrop:
   curl -X POST http://localhost:8080/api/v1/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["Text to encode goes here.", "text 2 here", " and text 3 here"]}' | jq '{texts, length, vector: .vector[:20]}'
