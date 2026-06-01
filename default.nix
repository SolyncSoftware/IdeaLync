{ bun2nix, ... }:

bun2nix.mkDerivation {
  pname = "idealync";
  version = "1.0.0";
  src = ./.;

  bunDeps = bun2nix.fetchBunDeps {
    bunNix = ./bun.nix;
  };

  module = "index.ts";
}
