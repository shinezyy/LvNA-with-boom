package lvna

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import boom.common._

class TokenBucketNode(implicit p: Parameters) extends LazyModule {
  val node = TLIdentityNode()
  lazy val module = new TokenBucketNodeImp(this)
}

class TokenBucketNodeImp(outer: TokenBucketNode) extends LazyModuleImp(outer) {
  val (bundleIn, _) = outer.node.in.unzip
  val (bundleOut, _) = outer.node.out.unzip

  val bucketIO = IO(Flipped(new BucketIO()))

  // require(bundleIn.size == 1, s"[TokenBucket] Only expect one link for a hart, current link count is ${bundleIn.size}")
  val (in, out) = (bundleIn zip bundleOut).head

  val phy = in.a.bits.address < p(MemInitAddr).U

  bucketIO.dsid := in.a.bits.dsid
  bucketIO.fire := out.a.ready && out.a.valid && !phy
  bucketIO.size := 1.U << in.a.bits.size

//  out.a.valid := in.a.valid && (phy || bucketIO.enable)
//  in.a.ready := out.a.ready && (phy || bucketIO.enable)
  out.a.valid := in.a.valid
  in.a.ready := out.a.ready
  if (DEBUG_TB_FETCH) {
    when(in.a.valid && !out.a.valid) {
      printf(p"request blocked by token bucket: 0x${Hexadecimal(in.a.bits.address)}\n")
    }
    when(out.a.valid && !in.a.ready) {
      printf(p"response blocked by token bucket: 0x${Hexadecimal(in.a.bits.address)}\n")
    }
  }
}
