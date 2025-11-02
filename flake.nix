{
  description = "NBB Ring";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            nodePackages_latest.nodejs
            babashka
            clj-kondo
            clojure
            clojure-lsp
          ];

          # Shell hook for additional environment setup
          shellHook = ''
            echo "Node version: $(node --version)"
          '';
        };
      }
    );
}
