package br.com.imoveisregla.core.model

// Portuguese UI labels for every enum the apps display.

val ListingType.label: String
    get() = when (this) {
        ListingType.APARTMENT -> "Apartamento"
        ListingType.HOUSE -> "Casa"
        ListingType.COMMERCIAL -> "Comercial"
        ListingType.LAND -> "Terreno"
    }

val ListingStatus.label: String
    get() = when (this) {
        ListingStatus.DRAFT -> "Rascunho"
        ListingStatus.LIVE -> "Ativo"
        ListingStatus.SOLD -> "Vendido"
        ListingStatus.WITHDRAWN -> "Retirado"
    }

val InquiryStage.label: String
    get() = when (this) {
        InquiryStage.INBOX -> "Novo"
        InquiryStage.QUALIFIED -> "Qualificado"
        InquiryStage.SHOWING -> "Visita"
        InquiryStage.OFFER -> "Proposta"
        InquiryStage.CLOSED_WON -> "Fechado"
        InquiryStage.CLOSED_LOST -> "Perdido"
    }

val Priority.label: String
    get() = when (this) {
        Priority.LOW -> "Baixa"
        Priority.MEDIUM -> "Média"
        Priority.HIGH -> "Alta"
    }

val ShowingStatus.label: String
    get() = when (this) {
        ShowingStatus.SCHEDULED -> "Agendada"
        ShowingStatus.CONFIRMED -> "Confirmada"
        ShowingStatus.ATTENDED -> "Realizada"
        ShowingStatus.NO_SHOW -> "Não compareceu"
        ShowingStatus.CANCELLED -> "Cancelada"
    }

val ApplicationStatus.label: String
    get() = when (this) {
        ApplicationStatus.SUBMITTED -> "Enviada"
        ApplicationStatus.UNDER_REVIEW -> "Em análise"
        ApplicationStatus.DOCS_REQUESTED -> "Documentos pendentes"
        ApplicationStatus.APPROVED -> "Aprovada"
        ApplicationStatus.REJECTED -> "Recusada"
        ApplicationStatus.WITHDRAWN -> "Cancelada"
    }

val ApplicationIntent.label: String
    get() = when (this) {
        ApplicationIntent.RENT -> "Alugar"
        ApplicationIntent.BUY -> "Comprar"
    }

val GuaranteeType.label: String
    get() = when (this) {
        GuaranteeType.SEGURO_FIANCA -> "Seguro fiança"
        GuaranteeType.FIADOR -> "Fiador"
        GuaranteeType.CAUCAO -> "Caução"
        GuaranteeType.TITULO_CAPITALIZACAO -> "Título de capitalização"
        GuaranteeType.SEM_GARANTIA -> "Sem garantia"
    }

val EmploymentType.label: String
    get() = when (this) {
        EmploymentType.CLT -> "CLT"
        EmploymentType.AUTONOMO -> "Autônomo"
        EmploymentType.EMPRESARIO -> "Empresário"
        EmploymentType.SERVIDOR -> "Servidor público"
        EmploymentType.APOSENTADO -> "Aposentado"
        EmploymentType.ESTUDANTE -> "Estudante"
        EmploymentType.OUTRO -> "Outro"
    }

val DocumentKind.label: String
    get() = when (this) {
        DocumentKind.RG_CNH -> "RG ou CNH"
        DocumentKind.CPF -> "CPF"
        DocumentKind.COMPROVANTE_RENDA -> "Comprovante de renda"
        DocumentKind.COMPROVANTE_RESIDENCIA -> "Comprovante de residência"
        DocumentKind.EXTRATO_BANCARIO -> "Extrato bancário"
        DocumentKind.IMPOSTO_RENDA -> "Declaração de IR"
        DocumentKind.OUTRO -> "Outro"
    }

val Affordability.label: String
    get() = when (this) {
        Affordability.OK -> "Cabe no orçamento"
        Affordability.TIGHT -> "Orçamento apertado"
        Affordability.OVER -> "Acima de 40% da renda"
        Affordability.UNKNOWN -> "Informe sua renda"
    }
