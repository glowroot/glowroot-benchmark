import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.concurrent.duration._

class HeatClinicSimulation extends Simulation {

  val duration = Integer.getInteger("duration", 0).toInt
  val iterations = Integer.getInteger("iterations", 1).toInt
  val users = Integer.getInteger("users").toInt

  val httpProtocol = http
    .baseURL("http://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .connection("keep-alive")

  def clickAround = exec(http("home").get("/"))
    .pause(5 milliseconds, 15 milliseconds)
    .exec(http("hot sauces").get("/hot-sauces"))
    .pause(5 milliseconds, 15 milliseconds)
    .exec(http("merchandise").get("/merchandise"))
    .pause(5 milliseconds, 15 milliseconds)
    .exec(http("clearance").get("/clearance"))
    .pause(5 milliseconds, 15 milliseconds)
    .exec(http("new to hot sauce").get("/new-to-hot-sauce"))
    .pause(5 milliseconds, 15 milliseconds)
    .exec(http("faq").get("/faq"))
    .pause(5 milliseconds, 15 milliseconds)

  if (duration != 0) {
    val scn = scenario("Heat Clinic").during(duration seconds) {
      exec(clickAround)
    }
    setUp(scn.inject(atOnceUsers(users)))
      .protocols(httpProtocol)
  } else {
    val scn = scenario("Heat Clinic").repeat(iterations) {
      exec(clickAround)
    }
    setUp(scn.inject(atOnceUsers(users)))
      .protocols(httpProtocol)
  }
}
