"""Integrazione HAndroid: gli add-on del telefono dentro Home Assistant.

Parla con il gestore add-on Kotlin (AddonApi) su 127.0.0.1:8099; il token sta nel file
`files/addons/token`, accanto alla cartella di configurazione, leggibile solo dall'app.
Nessuna patch a HA: handroid_boot copia questa cartella in config/custom_components a ogni avvio.
"""

import os
from datetime import timedelta
import logging

import aiohttp
import voluptuous as vol

from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant, ServiceCall
from homeassistant.exceptions import HomeAssistantError
from homeassistant.helpers import config_validation as cv
from homeassistant.helpers.aiohttp_client import async_get_clientsession
from homeassistant.helpers.update_coordinator import DataUpdateCoordinator, UpdateFailed

DOMAIN = "handroid"
URL = "http://127.0.0.1:8099"
PIATTAFORME = ["sensor", "switch"]
_LOGGER = logging.getLogger(__name__)


def token(hass):
    with open(os.path.join(os.path.dirname(hass.config.config_dir), "addons", "token"), encoding="utf-8") as fh:
        return fh.read().strip()


class Supervisore(DataUpdateCoordinator):
    """Legge l'elenco degli add-on ogni 30 s; le azioni passano da qui."""

    def __init__(self, hass):
        super().__init__(hass, _LOGGER, name=DOMAIN, update_interval=timedelta(seconds=30))
        self.sessione = async_get_clientsession(hass)
        self.token = token(hass)

    async def chiama(self, metodo, percorso, corpo=None, timeout=300):
        try:
            async with self.sessione.request(
                metodo, URL + percorso, json=corpo,
                headers={"Authorization": f"Bearer {self.token}"},
                timeout=aiohttp.ClientTimeout(total=timeout),
            ) as r:
                dati = await r.json(content_type=None)
        except (aiohttp.ClientError, TimeoutError) as e:
            raise HomeAssistantError(f"Supervisore HAndroid non raggiungibile: {e}") from e
        if r.status != 200:
            raise HomeAssistantError(dati.get("errore", f"HTTP {r.status}"))
        return dati

    async def _async_update_data(self):
        try:
            dati = await self.chiama("GET", "/addons", timeout=30)
        except HomeAssistantError as e:
            raise UpdateFailed(str(e)) from e
        return {a["slug"]: a for a in dati["addons"]} | {"__repository__": dati["repository"]}

    def addons(self):
        return {k: v for k, v in (self.data or {}).items() if not k.startswith("__")}

    async def azione(self, slug, azione, corpo=None):
        await self.chiama("POST", f"/addons/{slug}/{azione}", corpo)
        await self.async_request_refresh()


SLUG = vol.Schema({vol.Required("slug"): cv.string})


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry):
    sup = Supervisore(hass)
    await sup.async_config_entry_first_refresh()
    hass.data.setdefault(DOMAIN, {})[entry.entry_id] = sup

    def gestore(azione):
        async def servizio(call: ServiceCall):
            await sup.azione(call.data["slug"], azione)
        return servizio

    for nome, azione in (("install", "install"), ("start", "start"), ("stop", "stop"), ("uninstall", "uninstall")):
        hass.services.async_register(DOMAIN, nome, gestore(azione), schema=SLUG)

    async def opzioni(call: ServiceCall):
        corpo = {"options": call.data["options"]}
        if "boot_auto" in call.data:
            corpo["avvio_auto"] = call.data["boot_auto"]
        await sup.azione(call.data["slug"], "options", corpo)

    hass.services.async_register(DOMAIN, "set_options", opzioni, schema=vol.Schema({
        vol.Required("slug"): cv.string, vol.Required("options"): dict, vol.Optional("boot_auto"): cv.boolean}))

    async def repository(call: ServiceCall):
        await sup.chiama("DELETE" if call.data.get("remove") else "POST", "/repositories", {"url": call.data["url"]})
        await sup.async_request_refresh()

    hass.services.async_register(DOMAIN, "repository", repository, schema=vol.Schema({
        vol.Required("url"): cv.string, vol.Optional("remove", default=False): cv.boolean}))

    async def aggiorna(call: ServiceCall):
        await sup.chiama("POST", "/refresh")
        await sup.async_request_refresh()

    hass.services.async_register(DOMAIN, "refresh", aggiorna)

    await hass.config_entries.async_forward_entry_setups(entry, PIATTAFORME)
    return True


async def async_unload_entry(hass: HomeAssistant, entry: ConfigEntry):
    ok = await hass.config_entries.async_unload_platforms(entry, PIATTAFORME)
    if ok:
        hass.data[DOMAIN].pop(entry.entry_id)
    return ok
