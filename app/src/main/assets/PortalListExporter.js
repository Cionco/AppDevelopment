// ==UserScript==
// @id             iitc-plugin-listexport@OllieTerrance
// @name           IITC plugin: Portal List Exporter
// @category       Info
// @version        0.0.0.1
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @description    Exports a CSV list of all or enemy portals.
// @include        https://www.ingress.com/intel*
// @include        http://www.ingress.com/intel*
// @match          https://www.ingress.com/intel*
// @match          http://www.ingress.com/intel*
// @grant          none
// ==/UserScript==
// https://gist.github.com/OllieTerrance/d45c0a36b7c04b50d2ff#file-portallistexporter-user-js

function wrapper() {
    // in case IITC is not available yet, define the base plugin object
    if (typeof window.plugin !== "function") {
        window.plugin = function() {};
    }
    // base context for plugin
    window.plugin.listexport = function() {};
    var self = window.plugin.listexport;
    // check all portals in range, show result dialog
    self.gen = function gen(hit) {
        var o = ["Portal,Latitude,Longitude,Team,Level,Health,Resos,Links,Fields,Destroy AP,Deploy AP"]; 
        for (var x in window.portals) {
            var p = window.portals[x];
            var a = window.getPortalApGain(x);
            if (hit && ((PLAYER.team === "RESISTANCE" && p.options.team === TEAM_RES) || (PLAYER.team === "ENLIGHTENED" && p.options.team === TEAM_ENL))) continue;
            var l = window.getPortalLinks(x);
            if (!p.options || !p.options.data) continue;
            o.push([
                (p.options.data.title ? "\"" + p.options.data.title.replace(/\"/g, "\"\"") + "\"" : "\"?\""),
                p._latlng.lat,
                p._latlng.lng,
                ["NEU", "RES", "ENL"][p.options.team],
                p.options.data.level,
                p.options.data.health,
                p.options.data.resCount,
                l.in.length + l.out.length,
                window.getPortalFieldsCount(x),
                a.destroyAp + a.destroyResoAp,
                a.captureAp
            ].join(","));
        }
        var dialog = window.dialog({
            title: (hit ? "Hit" : "Portal") + " List: CSV export",
            // body must be wrapped in an outer tag (e.g. <div>content</div>)
            html: '<textarea id="listCSVExport" rows="30" style="width: 100%;"></textarea>'
        }).parent();
        $(".ui-dialog-buttonpane", dialog).remove();
        dialog.css("width", "800px")
            .css("top", ($(window).height() - dialog.height()) / 2)
            .css("left", ($(window).width() - dialog.width()) / 2);
        $("#listCSVExport").val(o.join("\n"));
        return dialog;
    }
    // setup function called by IITC
    self.setup = function init() {
        // add controls to toolbox
        $("#toolbox").append("<a onclick=\"window.plugin.listexport.gen(false);\" title=\"Generate a CSV list of all portals.\">Export All</a>");
        $("#toolbox").append("<a onclick=\"window.plugin.listexport.gen(true);\" title=\"Generate a CSV list of enemy portals.\">Hit List</a>");
        // delete setup to ensure init can't be run again
        delete self.setup;
    }
    // IITC plugin setup
    if (window.iitcLoaded && typeof self.setup === "function") {
        self.setup();
    } else if (window.bootPlugins) {
        window.bootPlugins.push(self.setup);
    } else {
        window.bootPlugins = [self.setup];
    }
}
// inject plugin into page
var script = document.createElement("script");
script.appendChild(document.createTextNode("(" + wrapper + ")();"));
(document.body || document.head || document.documentElement).appendChild(script);


