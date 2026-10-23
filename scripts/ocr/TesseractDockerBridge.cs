using System;
using System.Diagnostics;
using System.IO;
using System.Linq;

// Windows 上把标准 Tesseract 命令映射到本地 Docker；内容只挂载到禁用网络的临时容器。
class TesseractDockerBridge {
    static string Quote(string value) {
        // Windows 命令行参数规则：双引号前与结尾的反斜线须加倍。
        var text = new System.Text.StringBuilder("\"");
        int slashes = 0;
        foreach (char ch in value) {
            if (ch == '\\') { slashes++; continue; }
            if (ch == '\"') { text.Append('\\', slashes * 2 + 1).Append(ch); slashes = 0; continue; }
            text.Append('\\', slashes).Append(ch); slashes = 0;
        }
        return text.Append('\\', slashes * 2).Append('\"').ToString();
    }
    static int Main(string[] args) {
        if (args.Length < 2 || !File.Exists(args[0])) return 2;
        string input = Path.GetFullPath(args[0]);
        string output = Path.GetFullPath(args[1]);
        string directory = Path.GetDirectoryName(input);
        if (!String.Equals(directory, Path.GetDirectoryName(output), StringComparison.OrdinalIgnoreCase)) return 2;
        var command = new [] { "run", "--rm", "--network", "none", "--env", "OMP_THREAD_LIMIT=2",
            "--volume", directory + ":/ocr", "dfknowledge-tesseract:5", "timeout", "--signal=KILL", "110",
            "tesseract", "/ocr/" + Path.GetFileName(input), "/ocr/" + Path.GetFileName(output) }.Concat(args.Skip(2));
        try {
            var start = new ProcessStartInfo("docker", String.Join(" ", command.Select(Quote)));
            start.UseShellExecute = false;
            start.CreateNoWindow = true;
            using (var process = Process.Start(start)) {
                process.WaitForExit();
                return process.ExitCode;
            }
        } catch {
            Console.Error.WriteLine("本地 Docker OCR 启动失败，请检查 Docker 与 OCR 镜像。");
            return 1;
        }
    }
}
