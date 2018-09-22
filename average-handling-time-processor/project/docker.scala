import com.typesafe.sbt.packager.docker.{Cmd, CmdLike}

object DockerFile {
  val baseImage = "openjdk:8-jdk-alpine"

 def rewrite(
     original: Seq[CmdLike],
     userName: String,
     userId: Option[String],
     extraPackages: Seq[String]
 ): Seq[CmdLike] = {
     val fromLine = original.filter(_.makeContent.startsWith("FROM "))
     val rest = original.filterNot(_.makeContent.startsWith("FROM "))
     val installPackages = makeDockerInstallPackages(extraPackages ++ Seq("curl", "openssl", "bash"))
     val addUser = makeDockerUser(userName, userId)
     fromLine ++ installPackages ++ addUser ++ rest
 }
 
 def makeDockerInstallPackages(packages: Seq[String]): Seq[CmdLike] = {
     if (packages.nonEmpty) {
         Seq(Cmd("RUN", s"apk update && apk upgrade && apk add --update ${packages.toSet.mkString(" ")}"))
     } else {
         Seq(Cmd("RUN", "apk update && apk upgrade"))
     }
 }
 
 def makeDockerUser(user: String, uid: Option[String]) = {
     val group = user
     val gid = uid
     val gidArg = gid.map(g => s"-g $g").getOrElse("")
     val uidArg = uid.map(u => s"-u $u").getOrElse("")
     Seq(
         Cmd("RUN",
             s"if ! getent group $group ; then addgroup $gidArg $group ; fi && " +
             s"if ! id $user 2> /dev/null ; then adduser $uidArg -G $group -D -H $user ; fi"
             )
         )
 }
}
