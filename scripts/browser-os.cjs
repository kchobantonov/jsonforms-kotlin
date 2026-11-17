// Okio only needs a temporary-directory path while constructing its browser
// file-system placeholder. Browser bundles cannot access a real Node filesystem.
module.exports = { tmpdir: () => '/tmp' };
