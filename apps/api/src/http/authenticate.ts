import type { FastifyReply, FastifyRequest } from 'fastify';
import type { JwtUserPayload } from '../types/auth';

export async function authenticate(request: FastifyRequest, reply: FastifyReply) {
  try {
    await request.jwtVerify();
    const user = request.user as JwtUserPayload;

    request.auth = {
      tenantId: user.tenantId,
      userId: user.sub,
      companyId: user.companyId,
      branchId: user.branchId,
      email: user.email,
      role: user.role
    };
  } catch {
    return reply.code(401).send({ error: 'unauthorized', message: 'Sessao invalida ou expirada.' });
  }
}
