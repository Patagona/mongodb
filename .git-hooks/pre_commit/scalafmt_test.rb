module Overcommit::Hook::PreCommit
  class ScalafmtTest < Base
    MESSAGE_REGEX = /^\[(?<type>error|warn)\]\s+(?<file>.*) has changes after scalafmt$/

    def run
      result = execute(command, args: applicable_files)
      return :pass if result.success?

      [:fail, "You have non-formatted files, run scalafmt before commiting."]
    end
  end
end
