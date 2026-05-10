const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const port = 5500;

const contentTypes = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".css": "text/css; charset=utf-8",
};

http.createServer((req, res) => {
  const url = new URL(req.url, `http://127.0.0.1:${port}`);
  const requestPath = url.pathname === "/" ? "/test.html" : decodeURIComponent(url.pathname);
  const filePath = path.normalize(path.join(root, requestPath));

  if (!filePath.startsWith(root)) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  fs.readFile(filePath, (error, data) => {
    if (error) {
      res.writeHead(404);
      res.end("Not found");
      return;
    }

    res.writeHead(200, {
      "Content-Type": contentTypes[path.extname(filePath)] || "text/plain; charset=utf-8",
      "Cache-Control": "no-store",
    });
    res.end(data);
  });
}).listen(port, "127.0.0.1");
