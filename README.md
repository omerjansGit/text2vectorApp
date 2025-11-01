This project will be used mostly for ollama and text embeddings.


För att köra applikationen och testa:
1. installera ollama:
   
   https://ollama.com/download
3. dra ner modellen nomic-embed-text:latest:
   
   ollama pull nomic-embed-text
5. starta applikationen i intellij
6. gör en curl anrop:
   
   curl -X POST http://localhost:8080/api/v1/embed \
  -H "Content-Type: application/json" \
  -d '{"texts": ["Text to encode goes here.", "text 2 here", " and text 3 here"]}' | jq '{texts, length, vector: .vector[:20]}'

Eller lägg dina texter i filen text2vectorApp/src/main/resources/static/data.json och sen gör en curl så här:

curl -X POST http://localhost:8080/api/v1/embed \
  -H "Content-Type: application/json" \
  -d "@data.json" | jq '{texts, length, vector: .vector[:20]}'

## Usage Examples

### Single Document Upload
```bash
curl -X POST http://localhost:8080/api/v1/embed/document \
  -F "file=@document.pdf"
  
curl -X POST http://localhost:8080/api/v1/embed/document \
  -F "file=@document.docx"
  
curl -X POST http://localhost:8080/api/v1/embed/document \
  -F "file=@document.txt"
```

### Multiple Mixed File Types
```bash
curl -X POST http://localhost:8080/api/v1/embed/documents \
  -F "files=@document1.pdf" \
  -F "files=@document2.docx" \
  -F "files=@document3.txt"
```
