"use strict";

const express = require("express");
const fs = require("fs");
const { createRequire } = require("module");
const path = require("path");

function resolveApiBuild(rootDir) {
  const apiBuildPath = path.join(rootDir, "apps", "api", "dist", "server.js");
  if (!fs.existsSync(apiBuildPath)) {
    throw new Error(
      "build da API nao encontrado em business/volt_stock/apps/api/dist/server.js",
    );
  }
  return require(apiBuildPath);
}

function resolveNext(rootDir) {
  const webRequire = createRequire(
    path.join(rootDir, "apps", "web", "package.json"),
  );
  return webRequire("next");
}

async function createVoltStockApp() {
  const rootDir = __dirname;
  const webDir = path.join(rootDir, "apps", "web");
  const router = express.Router();
  const { buildServer } = resolveApiBuild(rootDir);
  const fastifyApp = await buildServer();

  await fastifyApp.ready();

  router.use("/api", (req, res) => {
    fastifyApp.server.emit("request", req, res);
  });

  const next = resolveNext(rootDir);
  const nextApp = next({
    dev: String(process.env.NODE_ENV || "").toLowerCase() !== "production",
    dir: webDir,
  });
  const handle = nextApp.getRequestHandler();
  await nextApp.prepare();

  router.use((req, res) => {
    req.url = req.originalUrl;
    return handle(req, res);
  });

  return router;
}

module.exports = createVoltStockApp;
