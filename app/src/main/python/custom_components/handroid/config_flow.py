"""Config flow (un'istanza, nessun campo) e options flow con il modulo generato dallo schema dell'add-on."""

import re

import voluptuous as vol

from homeassistant.config_entries import ConfigFlow, OptionsFlow
from homeassistant.exceptions import HomeAssistantError

DOMAIN = "handroid"
INTERVALLO = re.compile(r"(int|float)\((-?[0-9.]*),(-?[0-9.]*)\)")
LISTA = re.compile(r"list\((.+)\)")


def campo(tipo):
    """Tipo dello schema del manifest -> validatore voluptuous (stessi tipi di Opzioni.kt)."""
    m = INTERVALLO.fullmatch(tipo)
    if m:
        conv = int if m[1] == "int" else float
        return vol.All(vol.Coerce(conv), vol.Range(min=conv(m[2]) if m[2] else None, max=conv(m[3]) if m[3] else None))
    m = LISTA.fullmatch(tipo)
    if m:
        return vol.In(m[1].split("|"))
    return {"bool": bool, "int": vol.Coerce(int), "float": vol.Coerce(float),
            "port": vol.All(vol.Coerce(int), vol.Range(min=1, max=65535))}.get(tipo, str)


def modulo(schema, attuali):
    s = {}
    for k, t in schema.items():
        facoltativo = t.endswith("?")
        chiave = (vol.Optional if facoltativo else vol.Required)(k, default=attuali.get(k)) \
            if attuali.get(k) is not None else (vol.Optional if facoltativo else vol.Required)(k)
        s[chiave] = campo(t.rstrip("?"))
    return s


class HandroidConfigFlow(ConfigFlow, domain=DOMAIN):
    VERSION = 1

    async def async_step_user(self, user_input=None):
        await self.async_set_unique_id(DOMAIN)
        self._abort_if_unique_id_configured()
        if user_input is None:
            return self.async_show_form(step_id="user", data_schema=vol.Schema({}))
        return self.async_create_entry(title="Add-on HAndroid", data={})

    @staticmethod
    def async_get_options_flow(config_entry):
        return OpzioniAddon()


class OpzioniAddon(OptionsFlow):
    slug = None

    def _sup(self):
        return self.hass.data[DOMAIN][self.config_entry.entry_id]

    async def async_step_init(self, user_input=None):
        installati = {s: a["name"] for s, a in self._sup().addons().items() if a["installato"] and a.get("schema")}
        if not installati:
            return self.async_abort(reason="nessun_addon")
        if user_input is not None:
            self.slug = user_input["slug"]
            return await self.async_step_addon()
        return self.async_show_form(step_id="init", data_schema=vol.Schema({vol.Required("slug"): vol.In(installati)}))

    async def async_step_addon(self, user_input=None):
        a = self._sup().addons()[self.slug]
        errori = {}
        if user_input is not None:
            auto = user_input.pop("boot_auto", None)
            opzioni = {k: v for k, v in user_input.items() if v not in (None, "") or not a["schema"][k].endswith("?")}
            try:
                await self._sup().azione(self.slug, "options", {"options": opzioni, "avvio_auto": auto})
                return self.async_create_entry(data={})
            except HomeAssistantError as e:
                errori["base"] = "non_valide"
                self._errore = str(e)
        s = modulo(a["schema"], a.get("opzioni_attuali") or {})
        s[vol.Optional("boot_auto", default=a.get("avvio_auto", False))] = bool
        return self.async_show_form(step_id="addon", data_schema=vol.Schema(s), errors=errori,
                                    description_placeholders={"nome": a["name"], "errore": getattr(self, "_errore", "")})
