package com.example.upsccompanion.data

/**
 * The starter topic list. The id format "paper_subject_title" is the same one the first
 * version used, so progress saved by that version lines up with these topics.
 */
object DefaultSyllabus {
    private val entries = listOf(
        Triple("GS I", "History", listOf("Ancient India", "Medieval India", "Modern India", "The Freedom Struggle", "Post-independence India", "World History", "Art and Culture")),
        Triple("GS I", "Geography", listOf("Physical Geography", "Indian Geography", "World Geography", "Resources and Industries", "Geophysical Phenomena")),
        Triple("GS I", "Society", listOf("Indian Society", "Diversity", "Women and Population", "Poverty and Development", "Urbanisation", "Globalisation")),
        Triple("GS II", "Polity", listOf("Constitutional Framework", "Fundamental Rights and Duties", "Parliament", "Executive", "Judiciary", "Federalism", "Constitutional Bodies")),
        Triple("GS II", "Governance", listOf("Government Policies", "Transparency and Accountability", "E-governance", "Role of Civil Services", "Welfare Schemes")),
        Triple("GS II", "Social Justice", listOf("Vulnerable Sections", "Health", "Education", "Human Resources")),
        Triple("GS II", "International Relations", listOf("India and Neighbours", "Bilateral Relations", "International Institutions", "Global Groupings")),
        Triple("GS III", "Economy", listOf("National Income", "Inflation", "Banking and Monetary Policy", "Fiscal Policy", "External Sector", "Inclusive Growth")),
        Triple("GS III", "Agriculture", listOf("Cropping Patterns", "Agricultural Pricing and Subsidies", "Food Processing", "Land Reforms")),
        Triple("GS III", "Environment", listOf("Ecology and Biodiversity", "Climate Change", "Environmental Governance", "Pollution", "Conservation")),
        Triple("GS III", "Science and Technology", listOf("Space Technology", "Biotechnology", "AI and Emerging Technologies", "IT and Cyber Security")),
        Triple("GS III", "Security", listOf("Internal Security", "Border Management", "Cyber Security", "Organised Crime")),
        Triple("GS III", "Disaster Management", listOf("Disaster Management")),
        Triple("GS IV", "Ethics", listOf("Ethics and Human Values", "Attitude", "Aptitude", "Emotional Intelligence", "Thinkers", "Probity in Governance", "Case Studies")),
        Triple("Essay", "Essay", listOf("Essay Themes", "Introduction and Conclusion", "Examples and Illustrations", "Essay Practice")),
        Triple("CSAT", "CSAT", listOf("Reading Comprehension", "Logical Reasoning", "Basic Numeracy", "Data Interpretation"))
    )

    /** Order papers are shown in. Papers the user creates come after these, alphabetically. */
    val paperOrder = listOf("GS I", "GS II", "GS III", "GS IV", "Essay", "CSAT")

    fun topics(): List<SyllabusTopic> = entries.flatMap { (paper, subject, titles) ->
        titles.map { title -> SyllabusTopic(idFor(paper, subject, title), paper, subject, title) }
    }

    private val ids: Set<String> by lazy { topics().map { it.id }.toSet() }

    fun idFor(paper: String, subject: String, title: String) = "${paper}_${subject}_$title"
    fun isDefaultId(id: String) = id in ids

    fun sortPapers(papers: Collection<String>): List<String> =
        papers.distinct().sortedWith(compareBy<String> { paperOrder.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it })
}
