package no.nav.helse.sporbar

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private val log: Logger = LoggerFactory.getLogger("sporbar")

internal class NyttDokumentRiver(rapidsConnection: RapidsConnection, private val dokumentDao: DokumentDao) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("@id", "@opprettet") }
            validate { it.interestedIn("inntektsmeldingId", "id", "sykmeldingId") }
            validate { it.requireAny("@event_name", listOf("inntektsmelding", "ny_søknad", "sendt_søknad_nav", "sendt_søknad_arbeidsgiver")) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].textValue())
        val opprettet = packet["@opprettet"].asLocalDateTime()

        when (packet["@event_name"].textValue()) {
            "inntektsmelding" -> {
                val dokumentId = UUID.fromString(packet["inntektsmeldingId"].textValue())
                dokumentDao.opprett(hendelseId, opprettet, dokumentId, Dokument.Type.Inntektsmelding)
            }
            "ny_søknad", "sendt_søknad_nav", "sendt_søknad_arbeidsgiver" -> {
                val sykmeldingId = UUID.fromString(packet["sykmeldingId"].textValue())
                dokumentDao.opprett(hendelseId, opprettet, sykmeldingId, Dokument.Type.Sykmelding)
                val søknadId = UUID.fromString(packet["id"].textValue())
                dokumentDao.opprett(hendelseId, opprettet, søknadId, Dokument.Type.Søknad)
            }
            else -> throw IllegalStateException("Ukjent event (etter whitelist :mind_blown:)")
        }

        log.info("Dokument med hendelse $hendelseId lagret")
    }
}
