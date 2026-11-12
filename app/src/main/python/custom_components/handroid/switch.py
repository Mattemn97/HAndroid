"""Interruttore avvia/ferma per ogni add-on installato."""

from homeassistant.components.switch import SwitchEntity

from .entita import DOMAIN, EntitaAddon, aggiungi_nuovi


async def async_setup_entry(hass, entry, async_add_entities):
    sup = hass.data[DOMAIN][entry.entry_id]
    entry.async_on_unload(aggiungi_nuovi(sup, async_add_entities, InterruttoreAddon, lambda a: a["installato"]))


class InterruttoreAddon(EntitaAddon, SwitchEntity):
    _attr_name = "In esecuzione"
    _attr_icon = "mdi:play-pause"

    def __init__(self, sup, slug):
        super().__init__(sup, slug, "esecuzione")

    @property
    def available(self):
        return super().available and self.addon.get("installato", False)

    @property
    def is_on(self):
        return self.addon.get("stato") == "avviato"

    async def async_turn_on(self, **kwargs):
        await self.coordinator.azione(self.slug, "start")

    async def async_turn_off(self, **kwargs):
        await self.coordinator.azione(self.slug, "stop")
