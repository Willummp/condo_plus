@echo off
cd /d "%~dp0"
echo Iniciando servidor na porta 3000...
start "Condo+ Server" node -e "const h=require('http'),fs=require('fs'),p=require('path');h.createServer((q,r)=>{let f=p.join(process.cwd(),q.url==='/'?'/condo_plus_app.html':q.url);fs.readFile(f,(e,d)=>{r.writeHead(e?404:200,{'Content-Type':'text/html;charset=utf-8','Access-Control-Allow-Origin':'*'});r.end(e?'Not found':d)})}).listen(3000,()=>console.log('Servidor rodando: http://localhost:3000/condo_plus_app.html'))"
timeout /t 2 /nobreak >nul
start "" http://localhost:3000/condo_plus_app.html
echo Pronto! Pode fechar esta janela.
