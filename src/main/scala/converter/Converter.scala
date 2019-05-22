package converter

trait Converter {
  def xmlToJson(instruction: String): String
}
